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

package org.chaosfisch.youtubeuploader.plugins.socializeplugin.controller;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventTopicSubscriber;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.models.Queue;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.uploader.Uploader;
import org.chaosfisch.youtubeuploader.plugins.socializeplugin.models.Message;
import org.chaosfisch.youtubeuploader.plugins.socializeplugin.models.MessageTableModel;
import org.chaosfisch.youtubeuploader.plugins.socializeplugin.services.MessageService;
import org.chaosfisch.youtubeuploader.plugins.socializeplugin.services.Provider;
import org.chaosfisch.youtubeuploader.plugins.socializeplugin.services.providers.ISocialProvider;
import org.chaosfisch.youtubeuploader.services.settingsservice.spi.SettingsService;
import org.scribe.model.Token;

import javax.swing.table.TableModel;

/**
 * Created with IntelliJ IDEA.
 * User: Dennis
 * Date: 14.04.12
 * Time: 21:36
 * To change this template use File | Settings | File Templates.
 */
public class MessageController
{

	@Inject private MessageTableModel messageTableModel;
	@Inject private MessageService    messageService;
	@Inject private Injector          injector;
	@Inject private SettingsService   settingsService;

	public MessageController()
	{
		AnnotationProcessor.process(this);
	}

	public void setup()
	{
		for (final Message message : this.messageService.getAll()) {
			this.messageTableModel.addRow(message);
		}
	}

	public void addMessage(final int action, final Message data)
	{
		switch (action) {
			case 1:
				data.uploadid = null;
			case 0:
				this.messageService.create(data);
				break;
			case 2:
				this.publish(data);
				break;
		}
	}

	private void publish(final Message message)
	{
		this.publish(message.message, message.facebook, message.twitter, message.googleplus, message.youtube);
	}

	private void publish(final String message, final boolean facebook, final boolean twitter, final boolean googlePlus, final boolean youtube)
	{
		if (facebook) {
			final String settingsString = (String) this.settingsService.get("socialize.socialize.facebook", ""); //NON-NLS
			if (settingsString.contains("___")) {
				final String token = settingsString.substring(0, settingsString.indexOf("___"));
				final String secret = settingsString.substring(settingsString.indexOf("___") + 3, settingsString.length());
				final ISocialProvider facebookSocialProvider = this.messageService.get(Provider.FACEBOOK);
				facebookSocialProvider.setAccessToken(new Token(token, secret));
				facebookSocialProvider.publish(message);
			}
		}
		if (twitter) {
			final String settingsString = (String) this.settingsService.get("socialize.socialize.twitter", ""); //NON-NLS
			if (settingsString.contains("___")) {
				final String token = settingsString.substring(0, settingsString.indexOf("___"));
				final String secret = settingsString.substring(settingsString.indexOf("___") + 3, settingsString.length());
				final ISocialProvider twitterSocialProvider = this.messageService.get(Provider.TWITTER);
				twitterSocialProvider.setAccessToken(new Token(token, secret));
				twitterSocialProvider.publish(message);
			}
		}
	}

	public TableModel getMessageTableModel()
	{
		return this.messageTableModel;
	}

	public MessageService getMessageService()
	{
		return this.messageService;
	}

	@EventTopicSubscriber(topic = Uploader.UPLOAD_JOB_FINISHED)
	public void onUploadJobFinished(final String topic, final Queue queue)
	{
		final Message findParameter = new Message();
		findParameter.uploadid = queue.getIdentity();
		for (final Message message : this.messageService.find(findParameter)) {
			this.publish(message.message.replace("{video}", String.format("http://youtu.be/%s", queue.videoId)), message.facebook, message.twitter, message.googleplus, message.youtube); //NON-NLS
		}
		this.messageService.clearByUploadID(queue.getIdentity());
	}

	@EventTopicSubscriber(topic = Uploader.UPLOAD_FINISHED)
	public void onUploadsFinished(final String topic, final Object o)
	{
		for (final Message message : this.messageService.findWithoutQueueID()) {
			this.publish(message.message, message.facebook, message.twitter, message.googleplus, message.youtube);
		}

		this.messageService.clearByUploadID(null);
	}

	public void removeEntryAt(final int selectedRow)
	{
		if (this.messageTableModel.hasIndex(selectedRow)) {
			final Message message = this.messageTableModel.getRow(selectedRow);
			this.messageService.delete(message);
		}
	}
}
