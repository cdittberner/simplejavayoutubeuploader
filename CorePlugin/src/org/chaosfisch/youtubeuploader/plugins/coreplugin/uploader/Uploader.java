/*
 * Copyright (c) 2012, Dennis Fischer
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.chaosfisch.youtubeuploader.plugins.coreplugin.uploader;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.log4j.Logger;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventTopicSubscriber;
import org.chaosfisch.util.BetterSwingWorker;
import org.chaosfisch.util.Computer;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.models.Queue;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.services.spi.QueueService;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.uploader.worker.UploadFailed;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.uploader.worker.UploadWorker;
import org.chaosfisch.youtubeuploader.services.settingsservice.spi.SettingsService;
import org.chaosfisch.youtubeuploader.util.logger.InjectLogger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: Dennis
 * Date: 06.01.12
 * Time: 22:54
 * To change this template use File | Settings | File Templates.
 */
@SuppressWarnings("HardCodedStringLiteral")
public class Uploader
{
	public static final String ALLOWED   = "allowed";
	public static final String DENIED    = "denied";
	public static final String MODERATED = "moderated";

	public static final String UPLOAD_JOB_FINISHED = "uploadJobFinished";
	public static final String UPLOAD_ABORT        = "uploadAbort";
	public static final String UPLOAD_FAILED       = "uploadFailed";
	public static final String UPLOAD_LOG          = "uploadLog";
	public static final String UPLOAD_PROGRESS     = "uploadProgress";
	public static final String UPLOAD_STARTED      = "uploadStarted";
	public static final String UPLOAD_FINISHED     = "uploadFinished";
	public static final String UPLOAD_LIMIT        = "uploadLimit";
	public static final String QUEUE_START         = "queueStart";

	private final         ExecutorService executorService;
	@Inject private       QueueService    queueService;
	@Inject private       SettingsService settingsService;
	@Inject private       Injector        injector;
	@InjectLogger private Logger          logger;

	private static final long QUEUE_SLEEPTIME = 60000;

	private boolean inProgress;
	private short   runningUploads;
	private short maxUploads = 1;
	private short actionOnFinish;
	private int     speedLimit           = 1000 * 1024;
	private boolean startTimeCheckerFlag = true;

	public Uploader()
	{
		executorService = Executors.newFixedThreadPool(10);
		AnnotationProcessor.process(this);
	}

	public void start()
	{
		inProgress = true;

		new BetterSwingWorker()
		{
			@Override
			protected void background()
			{
				while (inProgress) {
					if (hasFreeUploadSpace()) {
						final Queue polled = queueService.poll();
						if (polled != null) {
							final UploadWorker uploadWorker = injector.getInstance(UploadWorker.class);
							uploadWorker.run(polled, speedLimit, 1048576 * Integer.parseInt((String) settingsService.get("coreplugin.general.CHUNK_SIZE", "10")));
							executorService.submit(uploadWorker);
							setSpeedLimit(speedLimit);
							runningUploads++;
						}
					}

					try {
						Thread.sleep(Uploader.QUEUE_SLEEPTIME);
					} catch (InterruptedException ignored) {
					}
				}
			}

			@Override
			protected void onDone()
			{

			}
		}.execute();
	}

	public void stop()
	{
		inProgress = false;
	}

	public void abort(final Queue queue)
	{
		EventBus.publish(Uploader.UPLOAD_ABORT, queue);
	}

	public boolean isRunning()
	{
		return inProgress && (runningUploads != 0);
	}

	private boolean hasFreeUploadSpace()
	{
		return runningUploads < maxUploads;
	}

	@EventTopicSubscriber(topic = Uploader.UPLOAD_JOB_FINISHED)
	public void onUploadJobFinished(final String topic, final Queue queue)
	{
		logger.info("Upload successful");
		uploadFinished(queue);
	}

	@EventTopicSubscriber(topic = Uploader.UPLOAD_FAILED)
	public void onUploadJobFailed(final String topic, final UploadFailed uploadFailed)
	{
		logger.info("Upload failed");
		uploadFinished(uploadFailed.getQueue());
	}

	private void uploadFinished(final Queue queue)
	{
		logger.info(String.format("Upload finished: %s; %s", queue.title, queue.videoId));
		runningUploads--;
		logger.info(String.format("Running uploads: %s", runningUploads));
		queueService.update(queue);
		if (queueService.getQueued().isEmpty() && (runningUploads == 0)) {
			logger.info("All uploads finished");
			final Timer timer = new Timer();
			timer.schedule(new TimerTask()
			{
				@Override public void run()
				{
					switch (actionOnFinish) {
						case 0:
							return;
						case 1:
							System.exit(0);
							return;
						case 2:
							Computer.shutdownComputer();
							return;
						case 3:
							Computer.hibernateComputer();
					}
				}
			}, 30000);
		}

		logger.info(String.format("Left uploads: %d", queueService.getQueued().size()));
	}

	public void setActionOnFinish(final short actionOnFinish)
	{
		this.actionOnFinish = actionOnFinish;
	}

	public void setSpeedLimit(final int bytes)
	{
		speedLimit = bytes * 1024;
		if (runningUploads > 0) {
			speedLimit = Math.round((bytes * 1024) / runningUploads);
			EventBus.publish(Uploader.UPLOAD_LIMIT, speedLimit);
		}
	}

	public void setMaxUploads(final short maxUploads)
	{
		if (maxUploads > 10) {
			this.maxUploads = 10;
		} else {
			this.maxUploads = maxUploads;
		}
	}

	public void exit()
	{
		executorService.shutdownNow();
	}

	public void runStarttimeChecker()
	{
		final BetterSwingWorker startTimeChecker = new BetterSwingWorker()
		{
			@Override
			protected void background()
			{
				while (!Thread.currentThread().isInterrupted() && startTimeCheckerFlag) {

					if (queueService.hasStarttime() && !inProgress) {
						start();
					}

					try {
						Thread.sleep(60000);
					} catch (InterruptedException ignored) {
					}
				}
			}

			@Override
			protected void onDone()
			{
				//To change body of implemented methods use File | Settings | File Templates.
			}
		};
		startTimeChecker.execute();
	}

	public void stopStarttimeChecker()
	{
		startTimeCheckerFlag = false;
	}
}