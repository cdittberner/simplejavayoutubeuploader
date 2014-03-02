/**************************************************************************************************
 * Copyright (c) 2014 Dennis Fischer.                                                             *
 * All rights reserved. This program and the accompanying materials                               *
 * are made available under the terms of the GNU Public License v3.0+                             *
 * which accompanies this distribution, and is available at                                       *
 * http://www.gnu.org/licenses/gpl.html                                                           *
 *                                                                                                *
 * Contributors: Dennis Fischer                                                                   *
 **************************************************************************************************/

package de.chaosfisch.youtube.playlist;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import de.chaosfisch.youtube.YouTubeFactory;
import de.chaosfisch.youtube.account.AccountModel;
import de.chaosfisch.youtube.account.IAccountService;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class YouTubePlaylistService implements IPlaylistService {
	private static final Logger logger            = LoggerFactory.getLogger(YouTubePlaylistService.class);
	private static final String DEFAULT_THUMBNAIL = "https://i.ytimg.com/vi/default.jpg";
	private static final long   MAX_PLAYLISTS     = 50L;

	private final SimpleMapProperty<AccountModel, SimpleListProperty<PlaylistModel>> playlistModelsProperty = new SimpleMapProperty<>(FXCollections
																																			  .observableHashMap());
	private final IAccountService accountService;

	public YouTubePlaylistService(final IAccountService accountService) {
		this.accountService = accountService;
	}

	@Override
	public void addVideoToPlaylist(final PlaylistModel playlist, final String videoId) throws IOException {

		final ResourceId resourceId = new ResourceId();
		resourceId.setKind("youtube#video");
		resourceId.setVideoId(videoId);

		final PlaylistItemSnippet playlistItemSnippet = new PlaylistItemSnippet();
		playlistItemSnippet.setPlaylistId(playlist.getYoutubeId());
		playlistItemSnippet.setResourceId(resourceId);

		final PlaylistItem playlistItem = new PlaylistItem();
		playlistItem.setSnippet(playlistItemSnippet);

		YouTubeFactory.getYouTube(playlist.getAccount())
				.playlistItems()
				.insert("snippet,status", playlistItem)
				.execute();

		logger.debug("Video added to playlist!");
	}

	@Override
	public void addYoutubePlaylist(final PlaylistModel playlist) throws IOException {
		logger.debug("Adding playlist {} to youtube.", playlist.getTitle());
		final PlaylistSnippet playlistSnippet = new PlaylistSnippet();
		playlistSnippet.setTitle(playlist.getTitle());
		playlistSnippet.setDescription(playlist.getDescription());

		final PlaylistStatus playlistStatus = new PlaylistStatus();
		playlistStatus.setPrivacyStatus(playlist.getPrivacyStatus() ? "private" : "public");

		final Playlist youTubePlaylist = new Playlist();
		youTubePlaylist.setSnippet(playlistSnippet);
		youTubePlaylist.setStatus(playlistStatus);

		YouTubeFactory.getYouTube(playlist.getAccount())
				.playlists()
				.insert("snippet,status", youTubePlaylist)
				.execute();
		logger.info("Added playlist to youtube");

	}

	@Override
	public void synchronizePlaylists(final List<AccountModel> accounts) throws IOException {
		logger.info("Synchronizing playlists.");
		for (final AccountModel account : accounts) {
			final YouTube.Playlists.List playlistsRequest = YouTubeFactory.getYouTube(account)
					.playlists()
					.list("id,snippet,contentDetails")
					.setMaxResults(MAX_PLAYLISTS)
					.setMine(true);

			String nextPageToken = "";
			final List<PlaylistModel> playlists = new ArrayList<>((int) MAX_PLAYLISTS);
			do {
				playlistsRequest.setPageToken(nextPageToken);
				final PlaylistListResponse response = playlistsRequest.execute();
				playlists.addAll(parsePlaylistListResponse(account, response));
				nextPageToken = response.getNextPageToken();
			} while (null != nextPageToken);

			logger.debug("Playlist synchronize okay.");
			final List<PlaylistModel> accountPlaylists = account.getPlaylists();
			accountPlaylists.removeAll(playlists);
			for (final PlaylistModel playlist : accountPlaylists) {
				delete(playlist);
			}
			account.getPlaylists().addAll(playlists);
			accountService.update(account);
		}
		logger.info("Playlists synchronized");
	}

	@Override
	public SimpleListProperty<PlaylistModel> playlistModelsProperty(final AccountModel accountModel) {
		return playlistModelsProperty.getOrDefault(accountModel, playlistModelsProperty.put(accountModel, new SimpleListProperty<>(FXCollections
																																		   .observableArrayList())));
	}

	List<PlaylistModel> parsePlaylistListResponse(final AccountModel account, final PlaylistListResponse response) {
		final ArrayList<PlaylistModel> list = new ArrayList<>(response.getItems()
																	  .size());
		for (final Playlist entry : response.getItems()) {
			final PlaylistModel playlist = findByPkey(entry.getId());
			if (null == playlist) {
				list.add(_createNewPlaylist(account, entry));
			} else {
				list.add(_updateExistingPlaylist(account, entry, playlist));
			}
		}
		return list;
	}

	PlaylistModel _updateExistingPlaylist(final AccountModel account, final Playlist entry, final PlaylistModel playlist) {
		setPlaylistModelInfos(account, entry, playlist);
		update(playlist);
		return playlist;
	}

	PlaylistModel _createNewPlaylist(final AccountModel account, final Playlist entry) {
		final PlaylistModel playlist = new PlaylistModel();
		playlist.setYoutubeId(entry.getId());
		setPlaylistModelInfos(account, entry, playlist);
		insert(playlist);
		return playlist;
	}

	private void setPlaylistModelInfos(final AccountModel account, final Playlist entry, final PlaylistModel playlist) {
		playlist.setTitle(entry.getSnippet().getTitle());
		playlist.setItemCount(entry.getContentDetails().getItemCount());
		playlist.setAccount(account);
		playlist.setDescription(entry.getSnippet().getDescription());
		final String thumbnailUrl = entry.getSnippet()
				.getThumbnails()
				.getHigh()
				.getUrl();
		playlist.setThumbnail(thumbnailUrl.equals(DEFAULT_THUMBNAIL) ? null : thumbnailUrl);
	}

	public ObservableMap<AccountModel, SimpleListProperty<PlaylistModel>> getPlaylistModelsProperty() {
		return playlistModelsProperty.get();
	}

	public void setPlaylistModelsProperty(final ObservableMap<AccountModel, SimpleListProperty<PlaylistModel>> playlistModelsProperty) {
		this.playlistModelsProperty.set(playlistModelsProperty);
	}
}
