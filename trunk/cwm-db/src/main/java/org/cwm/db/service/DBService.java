/*
 * Copyright 2011 CAST, Inc.
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
package org.cwm.db.service;

import net.databinder.hib.Databinder;
import net.databinder.hib.SessionUnit;

import org.hibernate.Session;

/**
 * An injectable service for interacting with Hibernate and cwm-db classes.
 * 
 * At the moment, this is just a facade in front of a few static methods provided 
 * by the Databinder class.  However, it may eventually become the primary way to 
 * access database functions.
 * 
 * @author bgoldowsky
 *
 */
public class DBService implements IDBService {

	public DBService() {
	}

	public Session getHibernateSession() {
		return Databinder.getHibernateSession();
	}

	public Object ensureHibernateSession(SessionUnit unit) {
		return Databinder.ensureSession(unit);
	}

}
