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
package org.cast.cwm.components;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.wicket.datetime.DateConverter;
import org.apache.wicket.datetime.markup.html.basic.DateLabel;
import org.apache.wicket.model.IModel;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ApproximateDateLabel extends DateLabel {

	private static final long serialVersionUID = 1L;

	public ApproximateDateLabel(String id, IModel<Date> mDate) {
		super(id, mDate, new ApproximateDateConverter());
	}

	protected static class ApproximateDateConverter extends DateConverter {

		private static final long serialVersionUID = 1L;
		
		// For dates that are today
		private static final String TODAY_PATTERN = "h:mma 'Today'";
		private static final String YESTERDAY_PATTERN =  "h:mma 'Yesterday'";
		private static final String THIS_YEAR_PATTERN =  "MMMM d";
		private static final String DEFAULT_PATTERN =  "MMMM d, yyyy";

		public ApproximateDateConverter() {
			super(true);
		}

		@Override
		public String convertToString(Date value, Locale locale)
		{
			DateTime dt = new DateTime(value.getTime(), getTimeZone());
			DateTimeFormatter format = getFormat(dt);

			if (getApplyTimeZoneDifference())
			{
				TimeZone zone = getClientTimeZone();
				if (zone != null)
				{
					// apply time zone to formatter
					format = format.withZone(DateTimeZone.forTimeZone(zone));
				}
			}
			return format.print(dt);
		}


		protected DateTimeFormatter getFormat(DateTime dt) {
			String pattern;
			DateTime now = new DateTime();
			// First check if it's yesterday - might be in a different year...
			if (now.minusDays(1).toDateMidnight().isEqual(dt.toDateMidnight())) {
				pattern = YESTERDAY_PATTERN;
			} else if (dt.getYear() == now.getYear()) {
				// same year
				if (dt.getDayOfYear() == now.getDayOfYear())
					pattern = TODAY_PATTERN;
				else
					pattern = THIS_YEAR_PATTERN;
			} else {
				pattern = DEFAULT_PATTERN;
			}
			return DateTimeFormat.forPattern(pattern).withLocale(getLocale()).withPivotYear(2000);
		}

		@Override
		public String getDatePattern(Locale locale) {
			throw new RuntimeException ("Shouldn't be called");
		}

		@Override
		protected DateTimeFormatter getFormat(Locale locale) {
			throw new RuntimeException ("Shouldn't be called");
		}
	}	
}