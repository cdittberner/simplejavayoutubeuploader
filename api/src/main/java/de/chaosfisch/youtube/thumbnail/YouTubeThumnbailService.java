/**************************************************************************************************
 * Copyright (c) 2014 Dennis Fischer.                                                             *
 * All rights reserved. This program and the accompanying materials                               *
 * are made available under the terms of the GNU Public License v3.0+                             *
 * which accompanies this distribution, and is available at                                       *
 * http://www.gnu.org/licenses/gpl.html                                                           *
 *                                                                                                *
 * Contributors: Dennis Fischer                                                                   *
 **************************************************************************************************/

package de.chaosfisch.youtube.thumbnail;

import com.google.api.client.http.InputStreamContent;
import de.chaosfisch.youtube.YouTubeFactory;
import de.chaosfisch.youtube.account.AccountModel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class YouTubeThumnbailService implements IThumbnailService {

	@Override
	public void upload(final File thumbnail, final String videoid, final AccountModel accountModel) throws IOException {
		try (final BufferedInputStream is = new BufferedInputStream(new FileInputStream(thumbnail))) {
			YouTubeFactory.getYouTube(accountModel)
					.thumbnails()
					.set(videoid, new InputStreamContent("application/octet-stream", is))
					.execute();
		}
	}
}
