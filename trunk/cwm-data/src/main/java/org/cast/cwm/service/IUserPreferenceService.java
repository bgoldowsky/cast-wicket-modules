/*
 * Copyright 2011-2013 CAST, Inc.
 *
 * This file is part of the CAST Wicket Modules:
 * see <http://code.google.com/p/cast-wicket-modules>.
 *
 * The CAST Wicket Modules are free software: you can redistribute and/or
 * modify them under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The CAST Wicket Modules are distributed in the hope that they will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.cast.cwm.service;

import org.apache.wicket.model.IModel;
import org.cast.cwm.data.User;


public interface IUserPreferenceService {

	/**
	 * Set the user preference.  Create it if it doesn't exist.
	 * @param mUser
	 * @param name
	 * @param booleanValue
	 */
	void setUserPreferenceBoolean(IModel<User> mUser, String name, Boolean booleanValue);

	/**
	 * Get the user preference by identifier.  Null if there is no preference
	 * 
	 * @param mUser
	 * @param identifier
	 * @return booleanValue
	 */
	Boolean getUserPreferenceBoolean(IModel<User> mUser, String name);

}
