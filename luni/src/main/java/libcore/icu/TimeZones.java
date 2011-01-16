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

package libcore.icu;

import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Provides access to ICU's time zone data.
 */
public final class TimeZones {
    private static final String[] availableTimeZones = TimeZone.getAvailableIDs();

    private TimeZones() {}

    /**
     * Implements TimeZone.getDisplayName by asking ICU.
     */
    public static String getDisplayName(String id, boolean daylight, int style, Locale locale) {
        // If we already have the strings, linear search through them is 10x quicker than
        // calling ICU for just the one we want.
        if (CachedTimeZones.locale.equals(locale)) {
            String result = lookupDisplayName(CachedTimeZones.names, id, daylight, style);
            if (result != null) {
                return result;
            }
        }
        return getDisplayNameImpl(id, daylight, style, locale.toString());
    }

    public static String lookupDisplayName(String[][] zoneStrings, String id, boolean daylight, int style) {
        for (String[] row : zoneStrings) {
            if (row[0].equals(id)) {
                if (daylight) {
                    return (style == TimeZone.LONG) ? row[3] : row[4];
                } else {
                    return (style == TimeZone.LONG) ? row[1] : row[2];
                }
            }
        }
        return null;
    }

    /**
     * Initialization holder for default time zone names. This class will
     * be preloaded by the zygote to share the time and space costs of setting
     * up the list of time zone names, so although it looks like the lazy
     * initialization idiom, it's actually the opposite.
     */
    private static class CachedTimeZones {
        /**
         * Name of default locale at the time this class was initialized.
         */
        private static final Locale locale = Locale.getDefault();

        /**
         * Names of time zones for the default locale.
         */
        private static final String[][] names = createZoneStringsFor(locale);
    }

    /**
     * Creates array of time zone names for the given locale.
     */
    private static String[][] createZoneStringsFor(Locale locale) {
        // Don't be distracted by the code for de-duplication below: this is the expensive bit!
        long start, nativeStart;
        start = nativeStart = System.currentTimeMillis();
        String[][] result = getZoneStringsImpl(locale.toString(), availableTimeZones);
        long nativeEnd = System.currentTimeMillis();

        // De-duplicate the strings (http://b/2672057).
        HashMap<String, String> internTable = new HashMap<String, String>();
        for (int i = 0; i < result.length; ++i) {
            for (int j = 1; j <= 4; ++j) {
                String original = result[i][j];
                String nonDuplicate = internTable.get(original);
                if (nonDuplicate == null) {
                    internTable.put(original, original);
                } else {
                    result[i][j] = nonDuplicate;
                }
            }
        }

        // Ending up in this method too often is an easy way to make your app slow, so we ensure
        // it's easy to tell from the log (a) what we were doing and (b) how long it took.
        long end = System.currentTimeMillis();
        long duration = end - start;
        long nativeDuration = nativeEnd - nativeStart;
        Logger.global.info("Loaded time zone names for " + locale + " in " + duration + "ms" +
                " (" + nativeDuration + "ms native).");

        return result;
    }

    /**
     * Returns an array of time zone strings, as used by DateFormatSymbols.getZoneStrings.
     */
    public static String[][] getZoneStrings(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }

        // TODO: We should force a reboot if the default locale changes.
        if (CachedTimeZones.locale.equals(locale)) {
            return clone2dStringArray(CachedTimeZones.names);
        }

        return createZoneStringsFor(locale);
    }

    public static String[][] clone2dStringArray(String[][] array) {
        String[][] result = new String[array.length][];
        for (int i = 0; i < array.length; ++i) {
            result[i] = array[i].clone();
        }
        return result;
    }

    /**
     * Returns an array containing the time zone ids in use in the country corresponding to
     * the given locale. This is not necessary for Java API, but is used by telephony as a
     * fallback.
     */
    public static String[] forLocale(Locale locale) {
        return forCountryCode(locale.getCountry());
    }

    private static native String[] forCountryCode(String countryCode);
    private static native String[][] getZoneStringsImpl(String locale, String[] timeZoneIds);
    private static native String getDisplayNameImpl(String id, boolean isDST, int style, String locale);
}
