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

package org.chaosfisch.youtubeuploader.plugins.coreplugin.services.impl;

import com.google.inject.Inject;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.apache.log4j.Logger;
import org.bushe.swing.event.EventBus;
import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventTopicSubscriber;
import org.chaosfisch.google.atom.Feed;
import org.chaosfisch.google.atom.VideoEntry;
import org.chaosfisch.google.auth.AuthenticationException;
import org.chaosfisch.google.auth.GoogleAuthorization;
import org.chaosfisch.google.auth.GoogleRequestSigner;
import org.chaosfisch.google.auth.RequestSigner;
import org.chaosfisch.google.request.HTTP_STATUS;
import org.chaosfisch.google.request.Request;
import org.chaosfisch.google.request.Response;
import org.chaosfisch.util.BetterSwingWorker;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.mappers.PlaylistMapper;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.mappers.PresetMapper;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.mappers.QueueMapper;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.models.Account;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.models.Playlist;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.models.Preset;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.models.Queue;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.services.spi.AccountService;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.services.spi.PlaylistService;
import org.chaosfisch.youtubeuploader.plugins.coreplugin.services.spi.YTService;
import org.chaosfisch.youtubeuploader.util.logger.InjectLogger;
import org.mybatis.guice.transactional.Transactional;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Dennis
 * Date: 13.01.12
 * Time: 15:32
 * To change this template use File | Settings | File Templates.
 */
public class PlaylistServiceImpl implements PlaylistService
{
	private static final String YOUTUBE_PLAYLIST_FEED_50_RESULTS = "http://gdata.youtube.com/feeds/api/users/default/playlists?v=2&max-results=50"; //NON-NLS

	@Inject private       PlaylistMapper playlistMapper;
	@Inject private       PresetMapper   presetMapper;
	@Inject private       QueueMapper    queueMapper;
	@InjectLogger private Logger         logger;
	private               boolean        synchronizeFlag;

	public PlaylistServiceImpl()
	{
		AnnotationProcessor.process(this);
	}

	@Transactional @Override public List<Playlist> getByAccount(final Account account)
	{
		return this.playlistMapper.findPlaylists(account);
	}

	@Transactional @Override public List<Playlist> getAll()
	{
		return this.playlistMapper.getAll();
	}

	@Transactional @Override public Playlist find(final Playlist playlist)
	{
		return this.playlistMapper.findPlaylist(playlist);
	}

	@Transactional @Override public Playlist create(final Playlist playlist)
	{
		this.playlistMapper.createPlaylist(playlist);
		EventBus.publish(PlaylistService.PLAYLIST_ENTRY_ADDED, playlist);
		return playlist;
	}

	@Transactional @Override public Playlist update(final Playlist playlist)
	{
		this.playlistMapper.updatePlaylist(playlist);
		EventBus.publish(PlaylistService.PLAYLIST_ENTRY_UPDATED, playlist);
		return playlist;
	}

	@Transactional @Override public Playlist delete(final Playlist playlist)
	{
		final List<Preset> presets = this.presetMapper.findByPlaylist(playlist);
		for (final Preset preset : presets) {
			preset.playlist = null;
			this.presetMapper.updatePreset(preset);
		}
		final List<Queue> queues = this.queueMapper.findByPlaylist(playlist);
		for (final Queue queue : queues) {
			queue.playlist = null;
			this.queueMapper.updateQueue(queue);
		}

		this.playlistMapper.deletePlaylist(playlist);
		EventBus.publish(PlaylistService.PLAYLIST_ENTRY_REMOVED, playlist);
		return playlist;
	}

	@Override
	public void synchronizePlaylists(final List<Account> accounts)
	{
		if (this.synchronizeFlag) {
			return;
		}
		this.logger.info("Synchronizing playlists.");
		this.synchronizeFlag = true;
		new BetterSwingWorker()
		{
			@Override
			protected void background()
			{
				final Request request;
				try {
					request = new Request.Builder(Request.Method.GET, new URL(PlaylistServiceImpl.YOUTUBE_PLAYLIST_FEED_50_RESULTS)).build();
				} catch (MalformedURLException ignored) {
					PlaylistServiceImpl.this.logger.warn(String.format("Malformed url playlist synchronize feed: %s", PlaylistServiceImpl.YOUTUBE_PLAYLIST_FEED_50_RESULTS));
					return;
				}
				for (final Account account : accounts) {

					final Response response;
					try {
						final Request tmpRequest = (Request) request.clone();
						PlaylistServiceImpl.this.getRequestSigner(account).sign(request);
						response = tmpRequest.send();
					} catch (AuthenticationException e) {
						e.printStackTrace();
						return;
					} catch (CloneNotSupportedException e) {
						e.printStackTrace();
						return;
					} catch (IOException e) {
						e.printStackTrace();
						return;
					}

					if (response.code == HTTP_STATUS.OK.getCode()) {
						PlaylistServiceImpl.this.logger.debug(String.format("Playlist synchronize okay. Code: %d, Message: %s, Body: %s", response.code, response.message, response.body));
						final Feed feed = PlaylistServiceImpl.this.parseFeed(response.body, Feed.class);

						if (feed.videoEntries == null) {
							PlaylistServiceImpl.this.logger.info("No playlists found.");
							return;
						}
						for (final VideoEntry entry : feed.videoEntries) {
							final Playlist playlist = new Playlist();
							playlist.title = entry.title;
							playlist.playlistKey = entry.playlistId;
							playlist.number = entry.playlistCountHint;
							playlist.url = entry.title;
							playlist.summary = entry.playlistSummary;
							playlist.account = account;
							PlaylistServiceImpl.this.createOrUpdate(playlist);
						}
					} else {
						PlaylistServiceImpl.this.logger.info(String.format("Playlist synchronize failed. Code: %d, Message: %s, Body: %s", response.code, response.message, response.body));
					}
				}
			}

			@Override protected void onDone()
			{
				EventBus.publish("playlistsSynchronized", null); //NON-NLS
				PlaylistServiceImpl.this.logger.info("Playlists synchronized");
				PlaylistServiceImpl.this.synchronizeFlag = false;
			}
		}.execute();
	}

	private void createOrUpdate(final Playlist playlist)
	{
		final Playlist searchObject = new Playlist();
		searchObject.playlistKey = playlist.playlistKey;
		final Playlist findObject = this.find(searchObject);
		if (!(findObject == null)) {
			playlist.identity = findObject.identity;
			this.update(playlist);
		} else {
			this.create(playlist);
		}
	}

	@Override public Playlist addYoutubePlaylist(final Playlist playlist)
	{
		final VideoEntry entry = new VideoEntry();
		entry.title = playlist.title;
		entry.playlistSummary = playlist.summary;
		final String atomData = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%s", this.parseObjectToFeed(entry)); //NON-NLS

		try {
			final Request request = new Request.Builder(Request.Method.POST, new URL("http://gdata.youtube.com/feeds/api/users/default/playlists")).build();
			this.getRequestSigner(playlist.account).sign(request);

			request.setContentType("application/atom+xml; charset=utf-8"); //NON-NLS

			final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(request.setContent());
			final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(bufferedOutputStream, Charset.forName("UTF-8"));
			try {
				outputStreamWriter.write(atomData);
				outputStreamWriter.flush();

				final Response response = request.send();
				this.logger.debug(String.format("Response-Playlist: %s, Code: %d, Message: %s, Body: %s", playlist.title, response.code, response.message, response.body));
				if ((response.code == HTTP_STATUS.OK.getCode()) || (response.code == HTTP_STATUS.CREATED.getCode())) {
					final List<Account> accountEntries = new LinkedList<Account>();
					accountEntries.add(playlist.account);
					this.synchronizePlaylists(accountEntries);
				}
			} catch (IOException e) {
				this.logger.debug("Failed adding Playlist! IOException", e);
			} finally {
				try {
					bufferedOutputStream.close();
					outputStreamWriter.close();
				} catch (IOException ignored) {
				}
			}
		} catch (MalformedURLException ex) {
			this.logger.debug("Failed adding Playlist! MalformedURLException", ex);
		} catch (IOException ex) {
			this.logger.debug("Failed adding Playlist! IOException", ex);
		} catch (AuthenticationException ignored) {
			this.logger.debug("Failed adding playlist! Not authenticated");
		}

		return null;
	}

	@Override public void addLatestVideoToPlaylist(final Playlist playlist, final String videoId)
	{
		try {
			final URL submitUrl = new URL("http://gdata.youtube.com/feeds/api/playlists/" + playlist.playlistKey);
			final VideoEntry submitFeed = new VideoEntry();
			submitFeed.id = videoId;
			submitFeed.mediaGroup = null;
			final String playlistFeed = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%s", this.parseObjectToFeed(submitFeed)); //NON-NLS

			final Request request = new Request.Builder(Request.Method.POST, submitUrl).build();
			this.getRequestSigner(playlist.account).sign(request);
			request.setContentType("application/atom+xml"); //NON-NLS

			final BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(request.setContent());
			final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(bufferedOutputStream, Charset.forName("UTF-8"));
			outputStreamWriter.write(playlistFeed); //NON-NLS
			outputStreamWriter.flush();

			final Response response = request.send();
			this.logger.debug(String.format("Video added to playlist! Videoid: %s, Playlist: %s, Code: %d, Message: %s, Body: %s", videoId, playlist.title, response.code, response.message, response.body));
		} catch (MalformedURLException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		} catch (AuthenticationException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		} catch (IOException e) {
			e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
		}
	}

	private <T> T parseFeed(final String atomData, final Class<T> clazz)
	{
		final XStream xStream = new XStream(new DomDriver("UTF-8"));
		xStream.processAnnotations(clazz);
		final Object o = xStream.fromXML(atomData);
		if (clazz.isInstance(o)) {
			return clazz.cast(o);
		}
		throw new IllegalArgumentException("atomData of invalid clazz object!");
	}

	private String parseObjectToFeed(final Object o)
	{
		final XStream xStream = new XStream(new DomDriver("UTF-8"));
		xStream.processAnnotations(o.getClass());
		return xStream.toXML(o);
	}

	private RequestSigner getRequestSigner(final Account account) throws AuthenticationException
	{
		return new GoogleRequestSigner(YTService.DEVELOPER_KEY, 2, new GoogleAuthorization(GoogleAuthorization.TYPE.CLIENTLOGIN, account.name, account.getPassword()));
	}

	@EventTopicSubscriber(topic = AccountService.ACCOUNT_ADDED) public void onAccountAdded(final String topic, final Account account)
	{
		final List<Account> accounts = new LinkedList<Account>();
		accounts.add(account);
		this.synchronizePlaylists(accounts);
	}
}
