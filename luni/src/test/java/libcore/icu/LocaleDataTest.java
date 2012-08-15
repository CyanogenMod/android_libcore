/*
 * Copyright (C) 2012 The Android Open Source Project
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

import java.util.Locale;

public class LocaleDataTest extends junit.framework.TestCase {
    public void testAll() throws Exception {
        // Test that we can get the locale data for all known locales.
        for (Locale l : Locale.getAvailableLocales()) {
            LocaleData d = LocaleData.get(l);
            System.err.println(l + " : " + d.yesterday + " " + d.today + " " + d.tomorrow);
        }
    }

    public void test_en_US() throws Exception {
        LocaleData l = LocaleData.get(Locale.US);
        assertEquals("AM", l.amPm[0]);
        assertEquals("BC", l.eras[0]);

        assertEquals("January", l.longMonthNames[0]);
        assertEquals("Jan", l.shortMonthNames[0]);
        assertEquals("J", l.tinyMonthNames[0]);

        assertEquals("January", l.longStandAloneMonthNames[0]);
        assertEquals("Jan", l.shortStandAloneMonthNames[0]);
        assertEquals("J", l.tinyStandAloneMonthNames[0]);

        assertEquals("Sunday", l.longWeekdayNames[1]);
        assertEquals("Sun", l.shortWeekdayNames[1]);
        assertEquals("S", l.tinyWeekdayNames[1]);

        assertEquals("Sunday", l.longStandAloneWeekdayNames[1]);
        assertEquals("Sun", l.shortStandAloneWeekdayNames[1]);
        assertEquals("S", l.tinyStandAloneWeekdayNames[1]);

        assertEquals("Yesterday", l.yesterday);
        assertEquals("Today", l.today);
        assertEquals("Tomorrow", l.tomorrow);
    }

    public void test_de_DE() throws Exception {
        LocaleData l = LocaleData.get(new Locale("de", "DE"));

        assertEquals("Gestern", l.yesterday);
        assertEquals("Heute", l.today);
        assertEquals("Morgen", l.tomorrow);
    }

    public void test_cs_CZ() throws Exception {
        LocaleData l = LocaleData.get(new Locale("cs", "CZ"));

        assertEquals("ledna", l.longMonthNames[0]);
        assertEquals("Led", l.shortMonthNames[0]);
        assertEquals("1", l.tinyMonthNames[0]);

        assertEquals("leden", l.longStandAloneMonthNames[0]);
        assertEquals("1.", l.shortStandAloneMonthNames[0]);
        assertEquals("l", l.tinyStandAloneMonthNames[0]);
    }

    public void test_ru_RU() throws Exception {
        LocaleData l = LocaleData.get(new Locale("ru", "RU"));

        assertEquals("воскресенье", l.longWeekdayNames[1]);
        assertEquals("вс", l.shortWeekdayNames[1]);
        assertEquals("В", l.tinyWeekdayNames[1]);

        // Russian stand-alone weekday names get an initial capital.
        assertEquals("Воскресенье", l.longStandAloneWeekdayNames[1]);
        assertEquals("Вс", l.shortStandAloneWeekdayNames[1]);
        assertEquals("В", l.tinyStandAloneWeekdayNames[1]);
    }
}
