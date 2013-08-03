/*
 * Copyright (c) 2013 Dennis Fischer.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0+
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors: Dennis Fischer
 */

package de.chaosfisch.uploader.guice;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Names;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import de.chaosfisch.google.GoogleModule;
import de.chaosfisch.google.auth.GDataRequestSigner;
import de.chaosfisch.google.auth.IGoogleRequestSigner;
import de.chaosfisch.google.youtube.thumbnail.IThumbnailService;
import de.chaosfisch.google.youtube.thumbnail.ThumbnailServiceImpl;
import de.chaosfisch.google.youtube.upload.Uploader;
import de.chaosfisch.google.youtube.upload.metadata.AbstractMetadataService;
import de.chaosfisch.google.youtube.upload.metadata.IMetadataService;
import de.chaosfisch.google.youtube.upload.resume.IResumeableManager;
import de.chaosfisch.google.youtube.upload.resume.ResumeableManagerImpl;
import de.chaosfisch.http.IRequestSigner;
import de.chaosfisch.http.RequestModule;
import de.chaosfisch.serialization.SerializationModule;
import de.chaosfisch.services.EnddirService;
import de.chaosfisch.services.impl.EnddirServiceImpl;
import de.chaosfisch.uploader.ApplicationData;
import de.chaosfisch.uploader.PersistenceConfiguration;
import de.chaosfisch.uploader.controller.UploadController;
import de.chaosfisch.uploader.persistence.PersistenceModule;
import de.chaosfisch.util.EventBusUtil;
import de.chaosfisch.util.TextUtil;
import javafx.stage.FileChooser;

import java.util.ResourceBundle;

public class GuiceBindings extends AbstractModule {

	@Override
	protected void configure() {
		install(new RequestModule());
		install(new SerializationModule());
		install(new GoogleModule());
		install(new PersistenceModule(new PersistenceConfiguration(ApplicationData.HOME, ApplicationData.HOME + "SimpleJavaYoutubeUploader/", "schema.sql", "/")));

		bind(ResourceBundle.class).annotatedWith(Names.named("i18n-resources"))
				.toInstance(ResourceBundle.getBundle("de.chaosfisch.uploader.resources.application"));

		bind(IRequestSigner.class).to(GDataRequestSigner.class).in(Singleton.class);
		bind(IGoogleRequestSigner.class).to(GDataRequestSigner.class).in(Singleton.class);

		mapCommands();
		mapServices();
		mapUtil();

		bind(Uploader.class).in(Singleton.class);
		bind(UploadController.class).in(Singleton.class);
	}

	private void mapUtil() {
		bind(FileChooser.class).in(Singleton.class);

		final EventBus eventBus = new EventBus();
		bind(EventBus.class).toInstance(eventBus);
		bindListener(Matchers.any(), new TypeListener() {
			@Override
			public <I> void hear(@SuppressWarnings("unused") final TypeLiteral<I> type, final TypeEncounter<I> encounter) {
				encounter.register(new InjectionListener<I>() {
					@Override
					public void afterInjection(final I injectee) {
						eventBus.register(injectee);
					}
				});
			}
		});

		requestStaticInjection(EventBusUtil.class);
		requestStaticInjection(TextUtil.class);
	}

	private void mapServices() {
		bind(IMetadataService.class).to(AbstractMetadataService.class).in(Singleton.class);
		bind(EnddirService.class).to(EnddirServiceImpl.class).in(Singleton.class);
		bind(IThumbnailService.class).to(ThumbnailServiceImpl.class).in(Singleton.class);
		bind(IResumeableManager.class).to(ResumeableManagerImpl.class);
	}

	private void mapCommands() {
		bind(ICommandProvider.class).to(CommandProvider.class);
	}

}