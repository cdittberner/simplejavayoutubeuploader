/**************************************************************************************************
 * Copyright (c) 2014 Dennis Fischer.                                                             *
 * All rights reserved. This program and the accompanying materials                               *
 * are made available under the terms of the GNU Public License v3.0+                             *
 * which accompanies this distribution, and is available at                                       *
 * http://www.gnu.org/licenses/gpl.html                                                           *
 *                                                                                                *
 * Contributors: Dennis Fischer                                                                   *
 **************************************************************************************************/

package de.chaosfisch.youtube.processors;

import de.chaosfisch.youtube.thumbnail.IThumbnailService;
import de.chaosfisch.youtube.upload.UploadModel;
import de.chaosfisch.youtube.upload.job.UploadeJobPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

class ThumbnailPostProcessor implements UploadeJobPostProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbnailPostProcessor.class);
	private final IThumbnailService thumbnailService;

	@Inject
	public ThumbnailPostProcessor(final IThumbnailService thumbnailService) {
		this.thumbnailService = thumbnailService;
	}

	@Override
	public UploadModel process(final UploadModel upload) {
		if (null != upload.getThumbnail()) {
			try {
				thumbnailService.upload(upload.getThumbnail(), upload.getVideoid(), upload.getAccount());
			} catch (final Exception e) {
				LOGGER.error("Thumbnail IOException", e);
			}
		}
		return upload;
	}
}
