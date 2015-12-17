/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package libcore.java.util;


import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import junit.framework.TestCase;

public class GregorianCalendarTest extends TestCase {

    private static final TimeZone LOS_ANGELES = TimeZone.getTimeZone("America/Los_Angeles");

    // Documented a previous difference in behavior between this and the RI, see
    // https://code.google.com/p/android/issues/detail?id=61993 for more details.
    // Switching to OpenJDK has fixed that issue and so this test has been changed to reflect
    // the correct behavior.
    public void test_computeFields_dayOfWeekAndWeekOfYearSet() {
        Calendar greg = new GregorianCalendar(LOS_ANGELES, Locale.ENGLISH);

        // Ensure we use different values to the default ones.
        int differentWeekOfYear = greg.get(Calendar.WEEK_OF_YEAR) == 1 ? 2 : 1;
        int differentDayOfWeek = greg.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                ? Calendar.TUESDAY : Calendar.MONDAY;

        // Setting WEEK_OF_YEAR and DAY_OF_WEEK with an intervening
        // call to computeFields will work.
        greg.set(Calendar.WEEK_OF_YEAR, differentWeekOfYear);
        assertEquals(differentWeekOfYear, greg.get(Calendar.WEEK_OF_YEAR));
        greg.set(Calendar.DAY_OF_WEEK, differentDayOfWeek);
        assertEquals(differentWeekOfYear, greg.get(Calendar.WEEK_OF_YEAR));

        // Setting WEEK_OF_YEAR after DAY_OF_WEEK with no intervening
        // call to computeFields will work.
        greg = new GregorianCalendar(LOS_ANGELES, Locale.ENGLISH);
        greg.set(Calendar.DAY_OF_WEEK, differentDayOfWeek);
        greg.set(Calendar.WEEK_OF_YEAR, differentWeekOfYear);
        assertEquals(differentWeekOfYear, greg.get(Calendar.WEEK_OF_YEAR));
        assertEquals(differentDayOfWeek, greg.get(Calendar.DAY_OF_WEEK));

        // Setting DAY_OF_WEEK after WEEK_OF_YEAR with no intervening
        // call to computeFields will work.
        greg = new GregorianCalendar(LOS_ANGELES, Locale.ENGLISH);
        greg.set(Calendar.WEEK_OF_YEAR, differentWeekOfYear);
        greg.set(Calendar.DAY_OF_WEEK, differentDayOfWeek);
        assertEquals(differentWeekOfYear, greg.get(Calendar.WEEK_OF_YEAR));
        assertEquals(differentDayOfWeek, greg.get(Calendar.DAY_OF_WEEK));
    }
}
