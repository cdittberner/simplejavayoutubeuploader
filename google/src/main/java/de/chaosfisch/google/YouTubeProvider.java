/**************************************************************************************************
 * Copyright (c) 2014 Dennis Fischer.                                                             *
 * All rights reserved. This program and the accompanying materials                               *
 * are made available under the terms of the GNU Public License v3.0+                             *
 * which accompanies this distribution, and is available at                                       *
 * http://www.gnu.org/licenses/gpl.html                                                           *
 *                                                                                                *
 * Contributors: Dennis Fischer                                                                   *
 **************************************************************************************************/

package de.chaosfisch.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.youtube.YouTube;
import de.chaosfisch.google.account.AccountModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YouTubeProvider implements Provider<YouTube> {
	private static final Logger                     LOGGER   = LoggerFactory.getLogger(YouTubeProvider.class);
	private final        Map<AccountModel, YouTube> services = new HashMap<>(1);
	private final HttpTransport httpTransport;
	private final JsonFactory   jsonFactory;
	private final Map<AccountModel, Credential> credentials = new HashMap<>(1);
	private AccountModel account;

	@Inject
	public YouTubeProvider(final HttpTransport httpTransport, final JsonFactory jsonFactory) {
		this.httpTransport = httpTransport;
		this.jsonFactory = jsonFactory;
	}

	@Override
	public YouTube get() {
		if (!services.containsKey(account)) {
			final Credential credential = getCredential(account);
			services.put(account, new YouTube.Builder(httpTransport, jsonFactory, request -> {
				request.setInterceptor(credential::intercept);
				request.setIOExceptionHandler(new HttpBackOffIOExceptionHandler(new ExponentialBackOff()));
			}).setApplicationName("simple-java-youtube-uploader").build());
		}
		return services.get(account);
	}

	public Credential getCredential(final AccountModel account) {
		if (!credentials.containsKey(account)) {
			final Credential credential = new GoogleCredential.Builder().setJsonFactory(jsonFactory)
					.setTransport(httpTransport)
					.setClientSecrets(GDataConfig.CLIENT_ID, GDataConfig.CLIENT_SECRET)
					.addRefreshListener(new CredentialRefreshListener() {
						@Override
						public void onTokenResponse(final Credential credential, final TokenResponse tokenResponse) throws IOException {
							LOGGER.info("Token refreshed");
						}

						@Override
						public void onTokenErrorResponse(final Credential credential, final TokenErrorResponse tokenErrorResponse) throws IOException {
							LOGGER.error("Token refresh error {}", tokenErrorResponse.toPrettyString());
						}
					})
					.build();
			credential.setRefreshToken(account.getRefreshToken());
			credentials.put(account, credential);
		}
		return credentials.get(account);
	}

	public YouTubeProvider setAccount(final AccountModel account) {
		this.account = account;
		return this;
	}
}