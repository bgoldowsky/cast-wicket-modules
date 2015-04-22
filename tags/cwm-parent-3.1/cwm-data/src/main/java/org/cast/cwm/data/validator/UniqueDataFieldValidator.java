/*
 * Copyright 2011-2014 CAST, Inc.
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
package org.cast.cwm.data.validator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import net.databinder.models.hib.CriteriaBuilder;
import net.databinder.models.hib.HibernateObjectModel;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.cast.cwm.data.PersistedObject;
import org.cast.cwm.data.User;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

/**
 * A validator for confirming field uniqueness for objects in the datastore.  For example:
 * <p>
 * <code>
 * new TextField<String>("username").add(new UniqeDataFieldValidator<String>(User.class, "username"))
 * </code>
 * </p>
 * will ensure that the username chosen in the text field is not already in use by another {@link User}.
 * If you are editing an existing user, you must indicate that user using the alternate 
 * constructor {@link #UniqueDataFieldValidator(IModel, String)}.  Otherwise, the validator will fail
 * because the value is in use by that user and will therefore not be unique if created again.
 * <br />
 * 
 * @author jbrookover
 *
 * @param <T>
 */
public class UniqueDataFieldValidator<T> extends AbstractValidator<T>{

	private static final long serialVersionUID = 1L;
	private Class<? extends PersistedObject> clazz;
	private IModel<? extends PersistedObject> current = new Model<PersistedObject>(null);
	private String field;
	private Map<String, IModel<? extends Serializable>> scope = new HashMap<String, IModel<? extends Serializable>>();

	/**
	 * Constructor used when editing a persistent object.
	 * 
	 * @param current the object being edited
	 * @param fieldName the field to confirm for uniqueness among objects of this type
	 */
	public UniqueDataFieldValidator(IModel<? extends PersistedObject> current, String fieldName) {
		this.current = current;
		this.clazz = current.getObject().getClass();
		this.field = fieldName;
	}
	
	/**
	 * Constructor used when creating a new persistent object.
	 * 
	 * @param clazz the type of object to check
	 * @param fieldName the field to confirm for uniqueness among objects of this type
	 */
	public UniqueDataFieldValidator(Class<? extends PersistedObject> clazz, String fieldName) {
		this.clazz = clazz;
		this.field = fieldName;
	}

	@Override
	protected void onValidate(final IValidatable<T> validatable) {
		HibernateObjectModel<PersistedObject> other = new HibernateObjectModel<PersistedObject>(clazz, new CriteriaBuilder() {

			private static final long serialVersionUID = 1L;

			@Override
			public void build(Criteria criteria) {
				criteria.add(Restrictions.eq(field, validatable.getValue()));
				for (String field : scope.keySet()) {
					criteria.add(Restrictions.eq(field, scope.get(field).getObject()));
				}
			}
		});	
		
		if (other.getObject() != null && !other.getObject().equals(current.getObject())) {
			error(validatable);
		}
	}
	
	/**
	 * Limit the scope of this validation.  Allows you to ensure that a
	 * data field is unique only within a certain group.  For example,
	 * <code>
	 * <pre>
	 * Site x = ...;
	 * validator.limitScope('site', siteModel);
	 * </pre>
	 * </code> 
	 * would limit the unique field validation to a given site, allowing
	 * you to have objects with non-unique fields in <i>different</i> sites.
	 * 
	 * @param field the name of the field in the object
	 * @param value the object that this field must represent
	 * @return this object, for chaining
	 */
	public UniqueDataFieldValidator<T> limitScope(String field, IModel<? extends Serializable> value) {
		scope.put(field, value);
		return this;
	}
	
	@Override 
	protected Map<String, Object> variablesMap(IValidatable<T> validatable) {
		Map<String, Object> map = super.variablesMap(validatable);
		map.put("field", field);
		map.put("object", clazz.getSimpleName());
		return map;
	}

}
