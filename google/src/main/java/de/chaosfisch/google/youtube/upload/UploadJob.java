/*
 * Copyright (c) 2013 Dennis Fischer.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0+
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors: Dennis Fischer
 */

package de.chaosfisch.google.youtube.upload;

import com.blogspot.nurkiewicz.asyncretry.AsyncRetryExecutor;
import com.blogspot.nurkiewicz.asyncretry.RetryContext;
import com.blogspot.nurkiewicz.asyncretry.RetryExecutor;
import com.blogspot.nurkiewicz.asyncretry.function.RetryRunnable;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.model.Video;
import com.google.common.base.Charsets;
import com.google.common.eventbus.EventBus;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import de.chaosfisch.google.account.IAccountService;
import de.chaosfisch.google.youtube.upload.events.UploadJobFinishedEvent;
import de.chaosfisch.google.youtube.upload.events.UploadJobProgressEvent;
import de.chaosfisch.google.youtube.upload.metadata.MetaBadRequestException;
import de.chaosfisch.google.youtube.upload.metadata.MetaLocationMissingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class UploadJob implements Callable<Upload> {

	private static final int     SC_OK                    = 200;
	private static final int     SC_CREATED               = 201;
	private static final int     SC_RESUME_INCOMPLETE     = 308;
	private static final int     SC_INTERNAL_SERVER_ERROR = 500;
	private static final int     SC_BAD_GATEWAY           = 502;
	private static final int     SC_SERVICE_UNAVAILABLE   = 503;
	private static final int     SC_GATEWAY_TIMEOUT       = 504;
	private static final int     DEFAULT_BUFFER_SIZE      = 65536;
	private static final int     MAX_DELAY                = 30000;
	private static final int     INITIAL_DELAY            = 5000;
	private static final double  MULTIPLIER_DELAY         = 2;
	private static final int     MAX_RETRIES              = 10;
	private static final Logger  LOGGER                   = LoggerFactory.getLogger(UploadJob.class);
	private static final Pattern RANGE_HEADER_SEPERATOR   = Pattern.compile("-");

	private final Set<UploadPreProcessor>  uploadPreProcessors;
	private final Set<UploadPostProcessor> uploadPostProcessors;
	private final EventBus                 eventBus;
	private final IUploadService           uploadService;
	private final IAccountService          accountService;
	private final RateLimiter              rateLimiter;

	private UploadJobProgressEvent uploadProgress;
	private Upload                 upload;
	private File                   fileToUpload;
	private long                   totalBytesUploaded;
	private long                   fileSize;
	private long                   start;
	private long                   end;
	private boolean                canceled;
	private boolean                retryed;
	private HttpURLConnection      request;

	@Inject
	private UploadJob(@Assisted final Upload upload, @Assisted final RateLimiter rateLimiter, final Set<UploadPreProcessor> uploadPreProcessors, final Set<UploadPostProcessor> uploadPostProcessors, final EventBus eventBus, final IUploadService uploadService, final IAccountService accountService) {
		this.upload = upload;
		this.rateLimiter = rateLimiter;
		this.uploadPreProcessors = uploadPreProcessors;
		this.uploadPostProcessors = uploadPostProcessors;
		this.eventBus = eventBus;
		this.uploadService = uploadService;
		this.accountService = accountService;
		this.eventBus.register(this);
	}

	@Override
	public Upload call() throws Exception {

		for (final UploadPreProcessor preProcessor : uploadPreProcessors) {
			try {
				upload = preProcessor.process(upload);
			} catch (Exception e) {
				LOGGER.error("Preprocessor error", e);
			}
		}

		final ScheduledExecutorService schedueler = Executors.newSingleThreadScheduledExecutor();
		final RetryExecutor executor = new AsyncRetryExecutor(schedueler).withExponentialBackoff(INITIAL_DELAY, MULTIPLIER_DELAY)
				.withMaxDelay(MAX_DELAY)
				.withMaxRetries(MAX_RETRIES)
				.retryOn(Exception.class)
				.abortOn(InterruptedException.class)
				.abortOn(CancellationException.class)
				.abortOn(ExecutionException.class)
				.abortOn(MetaBadRequestException.class)
				.abortOn(UploadFinishedException.class)
				.abortOn(UploadResponseException.class);
		try {
			// Schritt 1: Initialize
			initialize();
			// Schritt 2: MetadataUpload + UrlFetch
			executor.doWithRetry(metadata()).get();
			// Schritt 3: Upload
			executor.doWithRetry(upload()).get();

			for (final UploadPostProcessor postProcessor : uploadPostProcessors) {
				try {
					upload = postProcessor.process(upload);
				} catch (Exception e) {
					LOGGER.error("Postprocessor error", e);
				}
			}

			eventBus.post(new UploadJobFinishedEvent(upload));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Upload aborted / stopped.");
			upload.getStatus().setAborted(true);
		} catch (Exception e) {
			LOGGER.error("Upload error", e);
			upload.getStatus().setFailed(true);
		}

		upload.getStatus().setRunning(false);
		uploadService.update(upload);
		schedueler.shutdownNow();
		eventBus.unregister(this);
		canceled = true;
		return upload;
	}

	private void initialize() throws FileNotFoundException {
		// Set the time uploaded started
		final GregorianCalendar calendar = new GregorianCalendar();
		upload.setDateOfStart(calendar);
		uploadService.update(upload);

		// init vars
		fileToUpload = upload.getFile();
		fileSize = fileToUpload.length();
		totalBytesUploaded = 0;
		start = 0;
		end = fileSize - 1;

		if (!fileToUpload.exists()) {
			throw new FileNotFoundException("Datei existiert nicht.");
		}
	}

	private RetryRunnable metadata() {

		return new RetryRunnable() {
			@Override
			public void run(final RetryContext retryContext) throws MetaLocationMissingException, MetaBadRequestException, UploadFinishedException, UploadResponseException, IOException {
				if (null != upload.getUploadurl() && !upload.getUploadurl().isEmpty()) {
					LOGGER.info("Uploadurl existing: {}", upload.getUploadurl());
					resumeinfo();
				}

				upload.setUploadurl(uploadService.fetchUploadUrl(upload));
				uploadService.update(upload);

				// Log operation
				LOGGER.info("Uploadurl received: {}", upload.getUploadurl());
			}
		};
	}

	private RetryRunnable upload() {
		return new RetryRunnable() {

			@Override
			public void run(final RetryContext retryContext) throws Exception {
				try {
					if (null != retryContext.getLastThrowable()) {
						throw retryContext.getLastThrowable();
					}
				} catch (Throwable e) {
					LOGGER.error("Exception", e);
					resumeinfo();
				}
				uploadChunk();
			}
		};
	}

	private void uploadChunk() throws UploadResponseException, IOException {
		// Log operation
		LOGGER.debug("start={} end={} filesize={}", start, end, fileSize);

		// Log operation
		LOGGER.debug("Uploaded {} bytes so far, using PUT method.", totalBytesUploaded);

		if (null == uploadProgress) {
			uploadProgress = new UploadJobProgressEvent(upload, fileSize);
			uploadProgress.setTime(Calendar.getInstance().getTimeInMillis());
		}

		// Building PUT RequestImpl for chunk data
		final URL url = URI.create(upload.getUploadurl()).toURL();
		request = (HttpURLConnection) url.openConnection();
		request.setRequestMethod("PUT");
		request.setChunkedStreamingMode((int) (fileSize - start));
		request.setDoOutput(true);
		//Properties
		request.setRequestProperty("Content-Type", upload.getMimetype());
		request.setRequestProperty("Content-Length", String.format("%d", fileSize));
		if (retryed) {
			request.setRequestProperty("Content-Range", String.format("bytes %d-%d/%d", start, end, fileSize));
		}
		request.setRequestProperty("Authorization", accountService.getAuthentication(upload.getAccount()).getHeader());
		request.connect();

		try (final TokenOutputStream tokenOutputStream = new TokenOutputStream(request.getOutputStream())) {

			final InputSupplier<InputStream> fileInputSupplier = ByteStreams.slice(Files.newInputStreamSupplier(fileToUpload), start, end);
			ByteStreams.copy(fileInputSupplier, tokenOutputStream);
			tokenOutputStream.close();
			final int code = request.getResponseCode();
			switch (code) {
				case SC_OK:
				case SC_CREATED:

					final JsonFactory factory = new GsonFactory();
					final Video video = factory.fromInputStream(request.getInputStream(), Charsets.UTF_8, Video.class);
					LOGGER.debug("Upload created {} ", video.toPrettyString());

					upload.setVideoid(video.getId());
					upload.getStatus().setArchived(true);
					upload.getStatus().setFailed(false);
					uploadService.update(upload);
					break;
				case SC_RESUME_INCOMPLETE:
					System.out.println("Why is this called?");
					break;

				case SC_INTERNAL_SERVER_ERROR:
				case SC_BAD_GATEWAY:
				case SC_SERVICE_UNAVAILABLE:
				case SC_GATEWAY_TIMEOUT:
					throw new IOException(String.format("Unexepected response: %d", code));
				default:
					throw new UploadResponseException(code);
			}
		}
	}

	private void resumeinfo() throws UploadFinishedException, UploadResponseException, IOException {
		retryed = true;
		fetchResumeInfo(upload);

		LOGGER.info("Resuming stalled upload to: {}", upload.getUploadurl());

		totalBytesUploaded = start;
		// possibly rolling back the previously saved value
		fileSize = fileToUpload.length();
		LOGGER.info("Next byte to upload is {}-{}.", start, end);
	}

	private void fetchResumeInfo(final Upload upload) throws IOException, UploadFinishedException, UploadResponseException {
		final HttpResponse<String> response = Unirest.put(upload.getUploadurl())
				.header("Content-Range", String.format("bytes */%d", fileSize))
				.header("Authorization", accountService.getAuthentication(upload.getAccount()).getHeader())
				.asString();

		switch (response.getCode()) {
			case SC_CREATED:
				final JsonFactory factory = new GsonFactory();
				final Video video = factory.fromString(response.getBody(), Video.class);
				LOGGER.debug("Upload created {} ", video.toPrettyString());

				upload.setVideoid(video.getId());
				upload.getStatus().setArchived(true);
				upload.getStatus().setFailed(false);
				uploadService.update(upload);
				throw new UploadFinishedException();
			case SC_INTERNAL_SERVER_ERROR:
			case SC_BAD_GATEWAY:
			case SC_SERVICE_UNAVAILABLE:
			case SC_GATEWAY_TIMEOUT:
				throw new IOException(String.format("Unexepected response: %d", response.getCode()));
			default:
				throw new UploadResponseException(response.getCode());
			case SC_RESUME_INCOMPLETE:
				start = 0;
				end = fileSize - 1;

				if (!response.getHeaders().containsKey("Range")) {
					LOGGER.info("PUT to {} did not return Range-header.", upload.getUploadurl());
					LOGGER.info("Received headers: {}", response.getHeaders().toString());
				} else {
					LOGGER.info("Range header is: {}", response.getHeaders().get("Range"));

					final String[] parts = RANGE_HEADER_SEPERATOR.split(response.getHeaders().get("Range"));
					if (1 < parts.length) {
						start = Long.parseLong(parts[1]) + 1;
					}
				}
				if (response.getHeaders().containsKey("Location")) {
					LOGGER.info("New upload url is: {}", response.getHeaders().get("Location"));
					upload.setUploadurl(response.getHeaders().get("Location"));
					uploadService.update(upload);
				}
				break;
		}
	}

	private static class UploadFinishedException extends Exception {
		private static final long serialVersionUID = -9034528149972478083L;
	}

	private class TokenOutputStream extends BufferedOutputStream {

		public TokenOutputStream(final OutputStream outputStream) {
			super(outputStream, DEFAULT_BUFFER_SIZE);
		}

		@Override
		public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
			if (0 < rateLimiter.getRate()) {
				rateLimiter.acquire(b.length);
			}
			super.write(b, off, len);
			flush();
			if (canceled) {
				request.disconnect();
				throw new CancellationException("Cancled");
			}

			// Event Upload Progress
			// Calculate all uploadinformation
			totalBytesUploaded += b.length;
			final long diffTime = Calendar.getInstance().getTimeInMillis() - uploadProgress.getTime();
			if (1000 < diffTime) {
				uploadProgress.setBytes(totalBytesUploaded);
				uploadProgress.setTime(diffTime);
				eventBus.post(uploadProgress);
			}
		}
	}
}
