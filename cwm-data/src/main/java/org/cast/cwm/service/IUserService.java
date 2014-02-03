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

import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.cast.cwm.admin.EditUserPanel;
import org.cast.cwm.data.LoginSession;
import org.cast.cwm.data.Period;
import org.cast.cwm.data.Role;
import org.cast.cwm.data.User;
import org.cast.cwm.data.models.UserListModel;
import org.cast.cwm.service.UserService.LoginData;

public interface IUserService {
	
	/**
	 * Return the proper type of User objects for this application.
	 * Normally @User, but subclassses may override it with a customized subclass.
	 * @return class of User
	 */
	public Class<? extends User> getUserClass();

	/**
	 * Create a new transient User object of the correct class.
	 * (Applications may override this in a subclass to use a customized User class).
	 */
	public User newUser();

	/**
	 * Mark a user as valid in the database.  Commits changes
	 * to the database at the end of the call.  Any methods
	 * that override this should call the super method at
	 * the end. 
	 * 
	 * see: {@link EditUserPanel#setAutoConfirmNewUser(boolean)}
	 *
	 * @param user
	 */
	public void confirmUser(User user);

	public void generateSecurityToken(IModel<User> mUser);

	public IModel<List<User>> getAllUsers();

	public IModel<User> getById(long userId);

	public IModel<User> getByUsername(String username);

	public IModel<User> getBySubjectId(String subjectId);

	public IModel<User> getByEmail(String email);

	// gets both valid and invalid users
	public IModel<User> getAllByEmail(String email);

	public IModel<User> getByFullnameFromPeriod(String firstName,
			String lastName, IModel<Period> period);

	public UserListModel getByRole(Role role);

	public ISortableDataProvider<User,String> getUserListProvider();

	public ISortableDataProvider<User,String> getUserListProvider(
			IModel<Period> mPeriod);

	public ISortableDataProvider<User,String> getUserListProvider(IModel<Period> mPeriod, Role role);

	public ISortableDataProvider<User,String> getUncachedUserListProvider(
			IModel<Period> mPeriod);

	/**
	 * Return any open LoginSession for this user.
	 * (That is, where the end time is null). 
	 * @param user
	 * @return Model of a List of LoginSessions.
	 */
	public IModel<List<LoginSession>> getOpenLoginSessions(IModel<User> mUser);

	/**
	 * Return a list of all LoginSessions for the given User.
	 * @param mUser Model of a User
	 * @return Model of the list of LoginSessions.
	 */
	IModel<List<LoginSession>> getAllLoginSessions(IModel<User> mUser);

	/**
	 * Get an object that will give the total number of logins
	 * and the latest login date for a particular user.  This is a
	 * (somewhat) efficient query.
	 * 
	 * @param user
	 * @return
	 */
	public LoginData getLoginSessions(IModel<User> user);


}