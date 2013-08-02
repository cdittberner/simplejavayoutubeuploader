/*
 * Copyright (c) 2013 Dennis Fischer.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0+
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors: Dennis Fischer
 */

package de.chaosfisch.youtubeuploader.command;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import de.chaosfisch.youtubeuploader.db.Template;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class RemoveTemplateCommand extends Service<Void> {

	@Inject
	private TemplateDao templateDao;

	public Template template;

	@Override
	protected Task<Void> createTask() {
		return new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				Preconditions.checkNotNull(template);
				templateDao.delete(template);
				return null;
			}
		};
	}
}