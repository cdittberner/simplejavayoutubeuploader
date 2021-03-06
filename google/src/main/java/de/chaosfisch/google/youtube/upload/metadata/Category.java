/*
 * Copyright (c) 2014 Dennis Fischer.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0+
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors: Dennis Fischer
 */

package de.chaosfisch.google.youtube.upload.metadata;

import de.chaosfisch.google.atom.media.MediaCategory;
import de.chaosfisch.util.TextUtil;

public enum Category {
	FILM("Film", "category.film"), AUTOS("Autos", "category.autos"), MUSIC("Music", "category.music"), ANIMALS("Animals", "category.animals"), SPORTS("Sports", "category.sports"), TRAVEL("Travel", "category.travel"), GAMES("Games", "category.games"), PEOPLE("People", "category.people"), COMEDY("Comedy", "category.comedy"), ENTERTAINMENT("Entertainment", "category.entertainment"), NEWS("News", "category.news"), HOWTO("Howto", "category.howto"), EDUCATION("Education", "category.education"), TECH("Tech", "category.tech"), NONPROFIT("Nonprofit", "category.nonprofit");

	private final String term;
	private final String label;
	private final String scheme;

	private static final String DEFAULT_SCHEME = "http://gdata.youtube.com/schemas/2007/categories.cat";

	Category(final String term, final String label) {
		this(term, label, DEFAULT_SCHEME);
	}

	Category(final String term, final String label, final String scheme) {
		this.term = term;
		this.label = TextUtil.getString(label);
		this.scheme = scheme;
	}

	public MediaCategory toCategory() {
		return new MediaCategory(term, label, scheme);
	}

	@Override
	public String toString() {
		return label;
	}
}
