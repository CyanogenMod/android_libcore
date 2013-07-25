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
  private static final long MINUTE_DURATION = 60 * 1000;
  private static final long HOUR_DURATION = 60 * MINUTE_DURATION;
  private static final long DAY_DURATION = 24 * HOUR_DURATION;
  private static final long MONTH_DURATION = 31 * DAY_DURATION;
  private static final long YEAR_DURATION = 12 * MONTH_DURATION;

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

    assertEquals("Monday", formatDateRange(en_US, tz, fixedTime, fixedTime + HOUR_DURATION, FORMAT_SHOW_WEEKDAY));
    assertEquals("January 19", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR_DURATION, FORMAT_SHOW_DATE));
    assertEquals("3:30 AM", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_SHOW_TIME));
    assertEquals("January 19, 2009", formatDateRange(en_US, tz, fixedTime, fixedTime + HOUR_DURATION, FORMAT_SHOW_YEAR));
    assertEquals("January 19", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR_DURATION, 0 /*FORMAT_NO_YEAR*/));
    assertEquals("January", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR_DURATION, FORMAT_NO_MONTH_DAY));
    assertEquals("3:30 AM", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_12HOUR | FORMAT_SHOW_TIME));
    assertEquals("03:30", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_24HOUR | FORMAT_SHOW_TIME));
    assertEquals("3:30 AM", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_12HOUR /*| FORMAT_CAP_AMPM*/ | FORMAT_SHOW_TIME));
    assertEquals("12:00 PM", formatDateRange(en_US, tz, fixedTime + noonDuration, fixedTime + noonDuration, FORMAT_12HOUR | FORMAT_SHOW_TIME));
    assertEquals("12:00 PM", formatDateRange(en_US, tz, fixedTime + noonDuration, fixedTime + noonDuration, FORMAT_12HOUR | FORMAT_SHOW_TIME /*| FORMAT_CAP_NOON*/));
    assertEquals("12:00 PM", formatDateRange(en_US, tz, fixedTime + noonDuration, fixedTime + noonDuration, FORMAT_12HOUR /*| FORMAT_NO_NOON*/ | FORMAT_SHOW_TIME));
    assertEquals("12:00 AM", formatDateRange(en_US, tz, fixedTime - midnightDuration, fixedTime - midnightDuration, FORMAT_12HOUR | FORMAT_SHOW_TIME /*| FORMAT_NO_MIDNIGHT*/));
    assertEquals("3:30 AM", formatDateRange(en_US, tz, fixedTime, fixedTime, FORMAT_SHOW_TIME | FORMAT_UTC));
    assertEquals("3 AM", formatDateRange(en_US, tz, fixedTime - integralDuration, fixedTime - integralDuration, FORMAT_SHOW_TIME | FORMAT_ABBREV_TIME));
    assertEquals("Mon", formatDateRange(en_US, tz, fixedTime, fixedTime + HOUR_DURATION, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_WEEKDAY));
    assertEquals("Jan 19", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_MONTH));
    assertEquals("Jan 19", formatDateRange(en_US, tz, timeWithCurrentYear, timeWithCurrentYear + HOUR_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));

    assertEquals("1/19/2009", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * HOUR_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("1/19/2009 – 1/22/2009", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("1/19/2009 – 4/22/2009", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("1/19/2009 – 2/9/2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));

    assertEquals("19.1.2009", formatDateRange(de_DE, tz, fixedTime, fixedTime + HOUR_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19.01.2009 - 22.01.2009", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19.01.2009 - 22.04.2009", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19.01.2009 - 09.02.2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));

    assertEquals("1/19/2009", formatDateRange(es_US, tz, fixedTime, fixedTime + HOUR_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 22/1/2009", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 22/4/2009", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 9/2/2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));

    assertEquals("19/1/2009", formatDateRange(es_ES, tz, fixedTime, fixedTime + HOUR_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 22/1/2009", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 22/4/2009", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));
    assertEquals("19/1/2009 – 9/2/2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE));

    // These are some random other test cases I came up with.

    assertEquals("January 19–22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, 0));
    assertEquals("Jan 19–22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Mon, Jan 19 – Thu, Jan 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("Monday, January 19 – Thursday, January 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_WEEKDAY));

    assertEquals("January 19 – April 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, 0));
    assertEquals("Jan 19 – Apr 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Mon, Jan 19 – Wed, Apr 22", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("January–April", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_NO_MONTH_DAY));

    assertEquals("Jan 19, 2009 – Feb 9, 2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Jan 2009 – Feb 2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_NO_MONTH_DAY | FORMAT_ABBREV_ALL));
    assertEquals("January 19, 2009 – February 9, 2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, 0));
    assertEquals("Monday, January 19, 2009 – Thursday, February 9, 2012", formatDateRange(en_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_WEEKDAY));

    // The same tests but for de_DE.

    assertEquals("19.-22. Januar", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY_DURATION, 0));
    assertEquals("19.-22. Jan.", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Mo., 19. - Do., 22. Jan.", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("Montag, 19. - Donnerstag, 22. Januar", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_WEEKDAY));

    assertEquals("19. Januar - 22. April", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, 0));
    assertEquals("19. Jan. - 22. Apr.", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Mo., 19. Jan. - Mi., 22. Apr.", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("Januar-April", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_NO_MONTH_DAY));

    assertEquals("19. Jan. 2009 - 9. Feb. 2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("Jan. 2009 - Feb. 2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_NO_MONTH_DAY | FORMAT_ABBREV_ALL));
    assertEquals("19. Januar 2009 - 9. Februar 2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, 0));
    assertEquals("Montag, 19. Januar 2009 - Donnerstag, 9. Februar 2012", formatDateRange(de_DE, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_WEEKDAY));

    // The same tests but for es_US.

    assertEquals("19–22 enero", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, 0));
    assertEquals("19–22 ene", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("lun 19 ene – jue 22 ene", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("lunes 19 enero – jueves 22 enero", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_WEEKDAY));

    assertEquals("19 enero – 22 abril", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, 0));
    assertEquals("19 ene – 22 abr", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("lun 19 ene – mié 22 abr", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("enero–abril", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_NO_MONTH_DAY));

    assertEquals("19 ene 2009 – 9 feb 2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("ene 2009 – feb 2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_NO_MONTH_DAY | FORMAT_ABBREV_ALL));
    assertEquals("19 enero 2009 – 9 febrero 2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, 0));
    assertEquals("lunes 19 enero 2009 – jueves 9 febrero 2012", formatDateRange(es_US, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_WEEKDAY));

    // The same tests but for es_ES.

    assertEquals("19–22 enero", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY_DURATION, 0));
    assertEquals("19–22 ene", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("lun 19 ene – jue 22 ene", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("lunes 19 enero – jueves 22 enero", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * DAY_DURATION, FORMAT_SHOW_WEEKDAY));

    assertEquals("19 enero – 22 abril", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, 0));
    assertEquals("19 ene – 22 abr", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("lun 19 ene – mié 22 abr", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_SHOW_WEEKDAY | FORMAT_ABBREV_ALL));
    assertEquals("enero–abril", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * MONTH_DURATION, FORMAT_NO_MONTH_DAY));

    assertEquals("19 ene 2009 – 9 feb 2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
    assertEquals("ene 2009 – feb 2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_NO_MONTH_DAY | FORMAT_ABBREV_ALL));
    assertEquals("19 enero 2009 – 9 febrero 2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, 0));
    assertEquals("lunes 19 enero 2009 – jueves 9 febrero 2012", formatDateRange(es_ES, tz, fixedTime, fixedTime + 3 * YEAR_DURATION, FORMAT_SHOW_WEEKDAY));
  }

  public void test8862241() throws Exception {
    // Test the post-2038 future (http://b/8862241).
    TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
    long jan_19_2042 = new Date(2042 - 1900, 0, 19, 3, 30, 15).getTime();
    long oct_4_2046 = new Date(2046 - 1900, 9, 4, 3, 30, 15).getTime();
    assertEquals("Jan 19, 2042 – Oct 4, 2046", formatDateRange(Locale.US, tz, jan_19_2042, oct_4_2046, FORMAT_SHOW_DATE | FORMAT_ABBREV_ALL));
  }
}
