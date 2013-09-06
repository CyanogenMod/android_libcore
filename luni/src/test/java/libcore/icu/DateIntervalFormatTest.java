/*
 * Copyright (C) 2013 The Android Open Source Project
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

package libcore.icu;

import java.util.Arrays;
import java.util.Locale;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static libcore.icu.DateIntervalFormat.*;

public class DateIntervalFormatTest extends junit.framework.TestCase {
  private static final long MINUTE = 60 * 1000;
  private static final long HOUR = 60 * MINUTE;
  private static final long DAY = 24 * HOUR;
  private static final long MONTH = 31 * DAY;
  private static final long YEAR = 12 * MONTH;

  public void test_formatDateInterval() throws Exception {
    Date date = new Date(109, 0, 19, 3, 30, 15);
    long fixedTime = date.getTime();

    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    Date dateWithCurrentYear = new Date(currentYear - 1900, 0, 19, 3, 30, 15);
    long timeWithCurrentYear = dateWithCurrentYear.getTime();

    long noonDuration = (8 * 60 + 30) * 60 * 1000 - 15 * 1000;
    long midnightDuration = (3 * 60 + 30) * 60 * 1000 + 15 * 1000;
    long integralDuration = 30 * 60 * 1000 + 15 * 1000;

    // These are the old CTS tests for DateIntervalFormat.formatDateRange.

    Locale de_DE = new Locale("de", "DE");
    Locale en_US = new Locale("en", "US");
    Locale es_ES = new Locale("es", "ES");
    Locale es_US = new Locale("es", "US");

    TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");

    assertEquals("Monday", formatDateRange(en_US, tz, fixedTime, fixedTime + HOUR, FORMAT_SHOW_WEEKDAY));
    assertEquals("January 19", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR, FORMAT_SHOW_DATE));
    assertEquals("3:30 AM", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_SHOW_TIME));
    assertEquals("January 19, 2009", formatDateRange(en_US, tz, fixedTime, fixedTime + HOUR, FORMAT_SHOW_YEAR));
    assertEquals("January 19", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR, 0 /*FORMAT_NO_YEAR*/));
    assertEquals("January", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR, FORMAT_NO_MONTH_DAY));
    assertEquals("3:30 AM", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_12HOUR | FORMAT_SHOW_TIME));
    assertEquals("03:30", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_24HOUR | FORMAT_SHOW_TIME));
    assertEquals("3:30 AM", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_12HOUR /*| FORMAT_CAP_AMPM*/ | FORMAT_SHOW_TIME));
    assertEquals("12:00 PM", formatDateRange(en_US, tz, fixedTime + noonDuration, fixedTime + noonDuration, FORMAT_12HOUR | FORMAT_SHOW_TIME));
    assertEquals("12:00 PM", formatDateRange(en_US, tz, fixedTime + noonDuration, fixedTime + noonDuration, FORMAT_12HOUR | FORMAT_SHOW_TIME /*| FORMAT_CAP_NOON*/));
    assertEquals("12:00 PM", formatDateRange(en_US, tz, fixedTime + noonDuration, fixedTime + noonDuration, FORMAT_12HOUR /*| FORMAT_NO_NOON*/ | FORMAT_SHOW_TIME));
    assertEquals("12:00 AM", formatDateRange(en_US, tz, fixedTime - midnightDuration, fixedTime - midnightDuration, FORMAT_12HOUR | FORMAT_SHOW_TIME /*| FORMAT_NO_MIDNIGHT*/));
    assertEquals("3:30 AM", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_SHOW_TIME | FORMAT_UTC));
    assertEquals("3 AM", formatDateRange(en_US, tz, fixedTime - integralDuration, fixedTime - integralDuration, FORMAT_SHOW_TIME | FORMAT_ABBREV_TIME));
    assertEquals("Mon", formatDateRange(en_US, tz, fixedTime, fixedTime + HOUR, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_WEEKDAY));
    assertEquals("Jan 19", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR, FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH));
    assertEquals("Jan 19", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));

    assertEquals("1/19/2009", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * HOUR, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("1/19/2009 – 1/22/2009", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("1/19/2009 – 4/22/2009", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("1/19/2009 – 2/9/2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));

    assertEquals("19.1.2009", formatDateRange(de_DE, tz, fixedTime, fixedTime + HOUR, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19.01.2009 - 22.01.2009", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19.01.2009 - 22.04.2009", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19.01.2009 - 09.02.2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));

    assertEquals("1/19/2009", formatDateRange(es_US, tz, fixedTime, fixedTime + HOUR, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 22/1/2009", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 22/4/2009", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 9/2/2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));

    assertEquals("19/1/2009", formatDateRange(es_ES, tz, fixedTime, fixedTime + HOUR, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 22/1/2009", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 22/4/2009", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 9/2/2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));

    // These are some random other test cases I came up with.

    assertEquals("January 19–22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY, 0));
    assertEquals("Jan 19–22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Mon, Jan 19 – Thu, Jan 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("Monday, January 19 – Thursday, January 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_WEEKDAY));

    assertEquals("January 19 – April 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH, 0));
    assertEquals("Jan 19 – Apr 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Mon, Jan 19 – Wed, Apr 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("January–April", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_NO_MONTH_DAY));

    assertEquals("Jan 19, 2009 – Feb 9, 2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Jan 2009 – Feb 2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_NO_MONTH_DAY | FORMAT_ABBREV_ALL));
    assertEquals("January 19, 2009 – February 9, 2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR, 0));
    assertEquals("Monday, January 19, 2009 – Thursday, February 9, 2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_WEEKDAY));

    // The same tests but for de_DE.

    assertEquals("19.-22. Januar", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY, 0));
    assertEquals("19.-22. Jan.", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Mo., 19. - Do., 22. Jan.", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("Montag, 19. - Donnerstag, 22. Januar", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_WEEKDAY));

    assertEquals("19. Januar - 22. April", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH, 0));
    assertEquals("19. Jan. - 22. Apr.", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Mo., 19. Jan. - Mi., 22. Apr.", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("Januar-April", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_NO_MONTH_DAY));

    assertEquals("19. Jan. 2009 - 9. Feb. 2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Jan. 2009 - Feb. 2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_NO_MONTH_DAY | FORMAT_ABBREV_ALL));
    assertEquals("19. Januar 2009 - 9. Februar 2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR, 0));
    assertEquals("Montag, 19. Januar 2009 - Donnerstag, 9. Februar 2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_WEEKDAY));

    // The same tests but for es_US.

    assertEquals("19–22 enero", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY, 0));
    assertEquals("19–22 ene", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("lun 19 ene – jue 22 ene", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("lunes 19 enero – jueves 22 enero", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_WEEKDAY));

    assertEquals("19 enero – 22 abril", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH, 0));
    assertEquals("19 ene – 22 abr", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("lun 19 ene – mié 22 abr", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("enero–abril", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_NO_MONTH_DAY));

    assertEquals("19 ene 2009 – 9 feb 2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("ene 2009 – feb 2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_NO_MONTH_DAY | FORMAT_ABBREV_ALL));
    assertEquals("19 enero 2009 – 9 febrero 2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR, 0));
    assertEquals("lunes 19 enero 2009 – jueves 9 febrero 2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_WEEKDAY));

    // The same tests but for es_ES.

    assertEquals("19–22 enero", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY, 0));
    assertEquals("19–22 ene", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("lun 19 ene – jue 22 ene", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("lunes 19 enero – jueves 22 enero", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY, FORMAT_SHOW_WEEKDAY));

    assertEquals("19 enero – 22 abril", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH, 0));
    assertEquals("19 ene – 22 abr", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("lun 19 ene – mié 22 abr", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("enero–abril", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH, FORMAT_NO_MONTH_DAY));

    assertEquals("19 ene 2009 – 9 feb 2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("ene 2009 – feb 2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_NO_MONTH_DAY | FORMAT_ABBREV_ALL));
    assertEquals("19 enero 2009 – 9 febrero 2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR, 0));
    assertEquals("lunes 19 enero 2009 – jueves 9 febrero 2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR, FORMAT_SHOW_WEEKDAY));
  }

  public void test8862241() throws Exception {
    // Test the post-2038 future (http://b/8862241).
    TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
    long jan_19_2042 = new Date(2042 - 1900, 0, 19, 3, 30, 15).getTime();
    long oct_4_2046 = new Date(2046 - 1900, 9, 4, 3, 30, 15).getTime();
    assertEquals("Jan 19, 2042 – Oct 4, 2046", formatDateRange(Locale.US, tz, jan_19_2042, oct_4_2046, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
  }

  public void test10089890() throws Exception {
    // Test that we actually take the time zone into account.
    // The Unix epoch is UTC, so 0 is 1970-01-01T00:00Z...
    assertEquals("Jan 1–2", formatDateRange(0, DAY, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL, "Europe/London"));
    // But MTV is hours behind, so 0 was still the afternoon of the previous day...
    assertEquals("Dec 31, 1969 – Jan 1, 1970", formatDateRange(0, DAY, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL, "America/Los_Angeles"));
  }

  public void test10318326() throws Exception {
    long midnight = 0;
    long teaTime = 16 * HOUR;

    int time12 = FORMAT_12HOUR | FORMAT_SHOW_TIME;
    int time24 = FORMAT_24HOUR | FORMAT_SHOW_TIME;
    int abbr12 = time12 | FORMAT_ABBREV_ALL;
    int abbr24 = time24 | FORMAT_ABBREV_ALL;

    Locale l = Locale.US;
    TimeZone utc = TimeZone.getTimeZone("UTC");

    // Full length on-the-hour times.
    assertEquals("00:00", formatDateRange(l, utc, midnight, midnight, time24));
    assertEquals("12:00 AM", formatDateRange(l, utc, midnight, midnight, time12));
    assertEquals("16:00", formatDateRange(l, utc, teaTime, teaTime, time24));
    assertEquals("4:00 PM", formatDateRange(l, utc, teaTime, teaTime, time12));

    // Abbreviated on-the-hour times.
    assertEquals("00:00", formatDateRange(l, utc, midnight, midnight, abbr24));
    assertEquals("12 AM", formatDateRange(l, utc, midnight, midnight, abbr12));
    assertEquals("16:00", formatDateRange(l, utc, teaTime, teaTime, abbr24));
    assertEquals("4 PM", formatDateRange(l, utc, teaTime, teaTime, abbr12));

    // Abbreviated on-the-hour ranges.
    assertEquals("00:00–16:00", formatDateRange(l, utc, midnight, teaTime, abbr24));
    assertEquals("12 AM – 4 PM", formatDateRange(l, utc, midnight, teaTime, abbr12));

    // Abbreviated mixed ranges.
    assertEquals("00:00–16:01", formatDateRange(l, utc, midnight, teaTime + MINUTE, abbr24));
    assertEquals("12:00 AM – 4:01 PM", formatDateRange(l, utc, midnight, teaTime + MINUTE, abbr12));
  }

  public void test10560853_when_time_not_displayed() throws Exception {
    Locale l = Locale.US;
    TimeZone utc = TimeZone.getTimeZone("UTC");

    long midnight = 0;
    long midnightNext = 1 * DAY;

    int flags = FORMAT_SHOW_DATE | FORMAT_SHOW_WEEKDAY;

    // An all-day event runs until 0 milliseconds into the next day, but is formatted as if it's
    // just the first day.
    assertEquals("Thursday, January 1", formatDateRange(l, utc, midnight, midnightNext, flags));

    // Run one millisecond over, though, and you're into the next day.
    long nextMorning = 1 * DAY + 1;
    assertEquals("Thursday, January 1 – Friday, January 2", formatDateRange(l, utc, midnight, nextMorning, flags));

    // But the same reasoning applies for that day.
    long nextMidnight = 2 * DAY;
    assertEquals("Thursday, January 1 – Friday, January 2", formatDateRange(l, utc, midnight, nextMidnight, flags));
  }

  public void test10560853_for_single_day_events() throws Exception {
    Locale l = Locale.US;
    TimeZone utc = TimeZone.getTimeZone("UTC");

    int flags = FORMAT_SHOW_TIME | FORMAT_24HOUR | FORMAT_SHOW_DATE;

    assertEquals("January 1, 22:00–00:00", formatDateRange(l, utc, 22 * HOUR, 24 * HOUR, flags));
    assertEquals("January 1, 22:00 – January 2, 00:30", formatDateRange(l, utc, 22 * HOUR, 24 * HOUR + 30 * MINUTE, flags));
  }
}
