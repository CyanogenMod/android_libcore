/*
 * Copyright (C) 2015 The Android Open Source Project
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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import libcore.util.BasicLruCache;
import libcore.icu.DateIntervalFormat;
import com.ibm.icu.text.DisplayContext;
import com.ibm.icu.util.ULocale;

/**
 * Exposes icu4j's RelativeDateTimeFormatter.
 */
public final class RelativeDateTimeFormatter {

  // Values from public API in DateUtils to be used in this class. They must
  // match the ones in DateUtils.java.
  public static final int FORMAT_SHOW_TIME = 0x00001;
  public static final int FORMAT_SHOW_YEAR = 0x00004;
  public static final int FORMAT_NO_YEAR = 0x00008;
  public static final int FORMAT_SHOW_DATE = 0x00010;
  public static final int FORMAT_ABBREV_MONTH = 0x10000;
  public static final int FORMAT_NUMERIC_DATE = 0x20000;
  public static final int FORMAT_ABBREV_RELATIVE = 0x40000;
  public static final int FORMAT_ABBREV_ALL = 0x80000;

  public static final long SECOND_IN_MILLIS = 1000;
  public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
  public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
  public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
  public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;
  // YEAR_IN_MILLIS considers 364 days as a year. However, since this
  // constant comes from public API in DateUtils, it cannot be fixed here.
  public static final long YEAR_IN_MILLIS = WEEK_IN_MILLIS * 52;

  private static final FormatterCache CACHED_FORMATTERS = new FormatterCache();

  static class FormatterCache
      extends BasicLruCache<String, com.ibm.icu.text.RelativeDateTimeFormatter> {
    FormatterCache() {
      super(8);
    }
  };

  private RelativeDateTimeFormatter() {
  }

  /**
   * This is the internal API that implements the functionality of
   * DateUtils.getRelativeTimeSpanString(long, long, long, int), which is to
   * return a string describing 'time' as a time relative to 'now' such as
   * '5 minutes ago', or 'in 2 days'. More examples can be found in DateUtils'
   * doc.
   *
   * In the implementation below, it selects the appropriate time unit based on
   * the elapsed time between time' and 'now', e.g. minutes, days and etc.
   * Callers may also specify the desired minimum resolution to show in the
   * result. For example, '45 minutes ago' will become '0 hours ago' when
   * minResolution is HOUR_IN_MILLIS. Once getting the quantity and unit to
   * display, it calls icu4j's RelativeDateTimeFormatter to format the actual
   * string according to the given locale.
   *
   * Note that when minResolution is set to DAY_IN_MILLIS, it returns the
   * result depending on the actual date difference. For example, it will
   * return 'Yesterday' even if 'time' was less than 24 hours ago but falling
   * onto a different calendar day.
   *
   * It takes two additional parameters of Locale and TimeZone than the
   * DateUtils' API. Caller must specify the locale and timezone.
   * FORMAT_ABBREV_RELATIVE or FORMAT_ABBREV_ALL can be set in 'flags' to get
   * the abbreviated forms when available. When 'time' equals to 'now', it
   * always // returns a string like '0 seconds/minutes/... ago' according to
   * minResolution.
   */
  public static String getRelativeTimeSpanString(Locale locale, TimeZone tz, long time,
      long now, long minResolution, int flags) {
    if (locale == null) {
      throw new NullPointerException("locale == null");
    }
    if (tz == null) {
      throw new NullPointerException("tz == null");
    }
    long duration = Math.abs(now - time);
    boolean past = (now >= time);

    com.ibm.icu.text.RelativeDateTimeFormatter.Style style;
    if ((flags & (FORMAT_ABBREV_RELATIVE | FORMAT_ABBREV_ALL)) != 0) {
        style = com.ibm.icu.text.RelativeDateTimeFormatter.Style.SHORT;
    } else {
        style = com.ibm.icu.text.RelativeDateTimeFormatter.Style.LONG;
    }

    // We are currently using the _NONE and _FOR_BEGINNING_OF_SENTENCE for the
    // capitalization. We use _NONE for relative time strings, and the latter
    // to capitalize the first letter of strings that don't contain
    // quantities, such as "Yesterday", "Today" and etc. This is for backward
    // compatibility (see b/14493853).
    DisplayContext capitalizationContext = DisplayContext.CAPITALIZATION_NONE;

    com.ibm.icu.text.RelativeDateTimeFormatter.Direction direction;
    if (past) {
        direction = com.ibm.icu.text.RelativeDateTimeFormatter.Direction.LAST;
    } else {
        direction = com.ibm.icu.text.RelativeDateTimeFormatter.Direction.NEXT;
    }

    // 'relative' defaults to true as we are generating relative time span
    // string. It will be set to false when we try to display strings without
    // a quantity, such as 'Yesterday', etc.
    boolean relative = true;
    int count;
    com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit unit;
    com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit aunit = null;

    if (duration < MINUTE_IN_MILLIS && minResolution < MINUTE_IN_MILLIS) {
      count = (int)(duration / SECOND_IN_MILLIS);
      unit = com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit.SECONDS;
    } else if (duration < HOUR_IN_MILLIS && minResolution < HOUR_IN_MILLIS) {
      count = (int)(duration / MINUTE_IN_MILLIS);
      unit = com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit.MINUTES;
    } else if (duration < DAY_IN_MILLIS && minResolution < DAY_IN_MILLIS) {
      // Even if 'time' actually happened yesterday, we don't format it as
      // "Yesterday" in this case. Unless the duration is longer than a day,
      // or minResolution is specified as DAY_IN_MILLIS by user.
      count = (int)(duration / HOUR_IN_MILLIS);
      unit = com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit.HOURS;
    } else if (duration < WEEK_IN_MILLIS && minResolution < WEEK_IN_MILLIS) {
      count = Math.abs(DateIntervalFormat.dayDistance(tz, time, now));
      unit = com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit.DAYS;

      if (count == 2) {
        // Some locales have special terms for "2 days ago". Return them if
        // available. Note that we cannot set up direction and unit here and
        // make it fall through to use the call near the end of the function,
        // because for locales that don't have special terms for "2 days ago",
        // icu4j returns an empty string instead of falling back to strings
        // like "2 days ago".
        capitalizationContext = DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE;
        String str;
        if (past) {
          synchronized (CACHED_FORMATTERS) {
            str = getFormatter(locale.toString(), style, capitalizationContext)
                .format(
                    com.ibm.icu.text.RelativeDateTimeFormatter.Direction.LAST_2,
                    com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit.DAY);
          }
        } else {
          synchronized (CACHED_FORMATTERS) {
            str = getFormatter(locale.toString(), style, capitalizationContext)
                .format(
                    com.ibm.icu.text.RelativeDateTimeFormatter.Direction.NEXT_2,
                    com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit.DAY);
          }
        }
        if (str != null && !str.isEmpty()) {
          return str;
        }
        // Fall back to show something like "2 days ago". Reset the
        // capitalization setting.
        capitalizationContext = DisplayContext.CAPITALIZATION_NONE;
      } else if (count == 1) {
        // Show "Yesterday / Tomorrow" instead of "1 day ago / in 1 day".
        aunit = com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit.DAY;
        relative = false;
      } else if (count == 0) {
        // Show "Today" if time and now are on the same day.
        aunit = com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit.DAY;
        direction = com.ibm.icu.text.RelativeDateTimeFormatter.Direction.THIS;
        relative = false;
      }
    } else if (minResolution == WEEK_IN_MILLIS) {
      count = (int)(duration / WEEK_IN_MILLIS);
      unit = com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit.WEEKS;
    } else {
      // The duration is longer than a week and minResolution is not
      // WEEK_IN_MILLIS. Return the absolute date instead of relative time.

      // Bug 19822016:
      // If user doesn't supply the year display flag, we need to explicitly
      // set that to show / hide the year based on time and now. Otherwise
      // formatDateRange() would determine that based on the current system
      // time and may give wrong results.
      if ((flags & (FORMAT_NO_YEAR | FORMAT_SHOW_YEAR)) == 0) {
          Calendar timeCalendar = new GregorianCalendar(false);
          timeCalendar.setTimeZone(tz);
          timeCalendar.setTimeInMillis(time);
          Calendar nowCalendar = new GregorianCalendar(false);
          nowCalendar.setTimeZone(tz);
          nowCalendar.setTimeInMillis(now);

          if (timeCalendar.get(Calendar.YEAR) != nowCalendar.get(Calendar.YEAR)) {
              flags |= FORMAT_SHOW_YEAR;
          } else {
              flags |= FORMAT_NO_YEAR;
          }
      }

      return DateIntervalFormat.formatDateRange(locale, tz, time, time, flags);
    }

    if (relative) {
      synchronized (CACHED_FORMATTERS) {
        return getFormatter(locale.toString(), style, capitalizationContext)
            .format(count, direction, unit);
      }
    } else {
      capitalizationContext = DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE;
      synchronized (CACHED_FORMATTERS) {
        return getFormatter(locale.toString(), style, capitalizationContext)
            .format(direction, aunit);
      }
    }
  }

  /**
   * This is the internal API that implements
   * DateUtils.getRelativeDateTimeString(long, long, long, long, int), which is
   * to return a string describing 'time' as a time relative to 'now', formatted
   * like '[relative time/date], [time]'. More examples can be found in
   * DateUtils' doc.
   *
   * The function is similar to getRelativeTimeSpanString, but it always
   * appends the absolute time to the relative time string to return
   * '[relative time/date clause], [absolute time clause]'. It also takes an
   * extra parameter transitionResolution to determine the format of the date
   * clause. When the elapsed time is less than the transition resolution, it
   * displays the relative time string. Otherwise, it gives the absolute
   * numeric date string as the date clause. With the date and time clauses, it
   * relies on icu4j's RelativeDateTimeFormatter::combineDateAndTime() to
   * concatenate the two.
   *
   * It takes two additional parameters of Locale and TimeZone than the
   * DateUtils' API. Caller must specify the locale and timezone.
   * FORMAT_ABBREV_RELATIVE or FORMAT_ABBREV_ALL can be set in 'flags' to get
   * the abbreviated forms when they are available.
   *
   * Bug 5252772: Since the absolute time will always be part of the result,
   * minResolution will be set to at least DAY_IN_MILLIS to correctly indicate
   * the date difference. For example, when it's 1:30 AM, it will return
   * 'Yesterday, 11:30 PM' for getRelativeDateTimeString(null, null,
   * now - 2 hours, now, HOUR_IN_MILLIS, DAY_IN_MILLIS, 0), instead of '2
   * hours ago, 11:30 PM' even with minResolution being HOUR_IN_MILLIS.
   */
  public static String getRelativeDateTimeString(Locale locale, TimeZone tz, long time,
      long now, long minResolution, long transitionResolution, int flags) {

    if (locale == null) {
      throw new NullPointerException("locale == null");
    }
    if (tz == null) {
      throw new NullPointerException("tz == null");
    }

    // Get the time clause first.
    String timeClause = DateIntervalFormat.formatDateRange(locale, tz, time, time,
                                                           FORMAT_SHOW_TIME);

    long duration = Math.abs(now - time);
    // It doesn't make much sense to have results like: "1 week ago, 10:50 AM".
    if (transitionResolution > WEEK_IN_MILLIS) {
        transitionResolution = WEEK_IN_MILLIS;
    }
    com.ibm.icu.text.RelativeDateTimeFormatter.Style style;
    if ((flags & (FORMAT_ABBREV_RELATIVE | FORMAT_ABBREV_ALL)) != 0) {
        style = com.ibm.icu.text.RelativeDateTimeFormatter.Style.SHORT;
    } else {
        style = com.ibm.icu.text.RelativeDateTimeFormatter.Style.LONG;
    }

    // icu4j also has other options available to control the capitalization. We
    // are currently using the _NONE option only.
    DisplayContext capitalizationContext = DisplayContext.CAPITALIZATION_NONE;

    Calendar timeCalendar = new GregorianCalendar(false);
    timeCalendar.setTimeZone(tz);
    timeCalendar.setTimeInMillis(time);
    Calendar nowCalendar = new GregorianCalendar(false);
    nowCalendar.setTimeZone(tz);
    nowCalendar.setTimeInMillis(now);

    int days = Math.abs(DateIntervalFormat.dayDistance(timeCalendar, nowCalendar));

    // Now get the date clause, either in relative format or the actual date.
    String dateClause;
    if (duration < transitionResolution) {
      // This is to fix bug 5252772. If there is any date difference, we should
      // promote the minResolution to DAY_IN_MILLIS so that it can display the
      // date instead of "x hours/minutes ago, [time]".
      if (days > 0 && minResolution < DAY_IN_MILLIS) {
         minResolution = DAY_IN_MILLIS;
      }
      dateClause = getRelativeTimeSpanString(locale, tz, time, now, minResolution, flags);
    } else {
      // We always use fixed flags to format the date clause. User-supplied
      // flags are ignored.
      if (timeCalendar.get(Calendar.YEAR) != nowCalendar.get(Calendar.YEAR)) {
        // Different years
        flags = FORMAT_SHOW_DATE | FORMAT_SHOW_YEAR | FORMAT_NUMERIC_DATE;
      } else {
        // Default
        flags = FORMAT_SHOW_DATE | FORMAT_NO_YEAR | FORMAT_ABBREV_MONTH;
      }

      dateClause = DateIntervalFormat.formatDateRange(locale, tz, time, time, flags);
    }

    // Combine the two clauses, such as '5 days ago, 10:50 AM'.
    synchronized (CACHED_FORMATTERS) {
      return getFormatter(locale.toString(), style, capitalizationContext)
              .combineDateAndTime(dateClause, timeClause);
    }
  }

  /**
   * getFormatter() caches the RelativeDateTimeFormatter instances based on
   * the combination of localeName, sytle and capitalizationContext. It
   * should always be used along with the action of the formatter in a
   * synchronized block, because otherwise the formatter returned by
   * getFormatter() may have been evicted by the time of the call to
   * formatter->action().
   */
  private static com.ibm.icu.text.RelativeDateTimeFormatter getFormatter(
      String localeName, com.ibm.icu.text.RelativeDateTimeFormatter.Style style,
      DisplayContext capitalizationContext) {
    String key = localeName + "\t" + style + "\t" + capitalizationContext;
    com.ibm.icu.text.RelativeDateTimeFormatter formatter = CACHED_FORMATTERS.get(key);
    if (formatter == null) {
      formatter = com.ibm.icu.text.RelativeDateTimeFormatter.getInstance(
          new ULocale(localeName), null, style, capitalizationContext);
      CACHED_FORMATTERS.put(key, formatter);
    }
    return formatter;
  }
}
