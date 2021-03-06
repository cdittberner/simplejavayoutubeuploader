/*
 * Copyright (c) 2014 Dennis Fischer.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0+
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors: Dennis Fischer
 */

package de.chaosfisch.google.atom;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("atom:category")
class AtomCategory extends Category {

	@XStreamAlias("yt:assignable")
	public Object ytAssignable;

	@XStreamAlias("yt:browsable")
	public Object ytBrowsable;

	@XStreamAlias("yt:deprecated")
	public Object ytDeprecated;

	public AtomCategory() {
	}

	public AtomCategory(final String term, final String label, final String scheme) {
		this.term = term;
		this.label = label;
		this.scheme = scheme;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return label;
	}
}
