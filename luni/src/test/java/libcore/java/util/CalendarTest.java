/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.java.util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class CalendarTest extends junit.framework.TestCase {

    private static final TimeZone AMERICA_SAO_PAULO = TimeZone.getTimeZone("America/Sao_Paulo");

    // http://code.google.com/p/android/issues/detail?id=6184
    public void test_setTimeZone() {
        // The specific time zones don't matter; they just have to be different so we can see that
        // get(Calendar.ZONE_OFFSET) returns the zone offset of the time zone passed to setTimeZone.
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        assertEquals(0, cal.get(Calendar.ZONE_OFFSET));
        TimeZone tz = java.util.TimeZone.getTimeZone("GMT+7");
        cal.setTimeZone(tz);
        assertEquals(25200000, cal.get(Calendar.ZONE_OFFSET));
    }

    public void testAddOneDayOverDstForwardAdds23HoursAt0100() {
        Calendar calendar = new GregorianCalendar(AMERICA_SAO_PAULO);
        calendar.set(2011, 9, 15, 1, 0); // 01:00 GMT-3
        long hoursSinceEpoch = hoursSinceEpoch(calendar);
        calendar.add(Calendar.DATE, 1);
        assertEquals(23, hoursSinceEpoch(calendar) - hoursSinceEpoch);
        assertCalendarEquals(calendar, 2011, 9, 16, 1); // 01:00 GMT-2; +23 hours
    }

    /**
     * At their daylight savings time switch, Sao Paulo changes from
     * "00:00 GMT-3" to "01:00 GMT-2". When adding time across this boundary,
     * drop an hour to keep the hour+minute constant unless that prevents the
     * date field from being incremented.
     * http://code.google.com/p/android/issues/detail?id=17502
     */
    public void testAddOneDayOverDstForwardAdds24HoursAt0000() {
        Calendar calendar = new GregorianCalendar(AMERICA_SAO_PAULO);
        calendar.set(2011, 9, 15, 0, 0); // 00:00 GMT-3
        long hoursSinceEpoch = hoursSinceEpoch(calendar);
        calendar.add(Calendar.DATE, 1);
        assertEquals(24, hoursSinceEpoch(calendar) - hoursSinceEpoch);
        assertCalendarEquals(calendar, 2011, 9, 16, 1); // 01:00 GMT-2; +24 hours
    }

    public void testAddOneDayOverDstBackAdds25HoursAt0000() {
        Calendar calendar = new GregorianCalendar(AMERICA_SAO_PAULO);
        calendar.set(2011, 1, 19, 0, 0); // 00:00 GMT-2
        long hoursSinceEpoch = hoursSinceEpoch(calendar);
        calendar.add(Calendar.DATE, 1);
        assertEquals(25, hoursSinceEpoch(calendar) - hoursSinceEpoch);
        assertCalendarEquals(calendar, 2011, 1, 20, 0); // 00:00 GMT-3; +25 hours
    }

    public void testAddOneDayOverDstBackAdds25HoursAt0100() {
        Calendar calendar = new GregorianCalendar(AMERICA_SAO_PAULO);
        calendar.set(2011, 1, 19, 1, 0); // 00:00 GMT-2
        long hoursSinceEpoch = hoursSinceEpoch(calendar);
        calendar.add(Calendar.DATE, 1);
        assertEquals(25, hoursSinceEpoch(calendar) - hoursSinceEpoch);
        assertCalendarEquals(calendar, 2011, 1, 20, 1); // 00:00 GMT-3; +25 hours
    }

    public void testAddTwoHalfDaysOverDstForwardAdds23HoursAt0100() {
        Calendar calendar = new GregorianCalendar(AMERICA_SAO_PAULO);
        calendar.set(2011, 9, 15, 1, 0); // 01:00 GMT-3
        long hoursSinceEpoch = hoursSinceEpoch(calendar);
        calendar.add(Calendar.AM_PM, 2);
        assertEquals(23, hoursSinceEpoch(calendar) - hoursSinceEpoch);
        assertCalendarEquals(calendar, 2011, 9, 16, 1); // 01:00 GMT-2; +23 hours
    }

    public void testAdd24HoursOverDstForwardAdds24Hours() {
        Calendar calendar = new GregorianCalendar(AMERICA_SAO_PAULO);
        calendar.set(2011, 9, 15, 1, 0); // 01:00 GMT-3
        long hoursSinceEpoch = hoursSinceEpoch(calendar);
        calendar.add(Calendar.HOUR, 24);
        assertEquals(24, hoursSinceEpoch(calendar) - hoursSinceEpoch);
        assertCalendarEquals(calendar, 2011, 9, 16, 2); // 02:00 GMT-2; +24 hours
    }

    private void assertCalendarEquals(Calendar calendar, int year, int month, int day, int hour) {
        assertEquals(year, calendar.get(Calendar.YEAR));
        assertEquals(month, calendar.get(Calendar.MONTH));
        assertEquals(day, calendar.get(Calendar.DATE));
        assertEquals(hour, calendar.get(Calendar.HOUR_OF_DAY));
    }

    private static long hoursSinceEpoch(Calendar c) {
        long ONE_HOUR = 3600L * 1000L;
        return c.getTimeInMillis() / ONE_HOUR;
    }
}
