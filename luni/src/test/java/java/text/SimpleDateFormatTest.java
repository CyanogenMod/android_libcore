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

package java.text;

import java.util.Date;

public class SimpleDateFormatTest extends junit.framework.TestCase {
    // The RI fails this test.
    public void test2DigitYearStartIsCloned() throws Exception {
        // Test that get2DigitYearStart returns a clone.
        SimpleDateFormat sdf = new SimpleDateFormat();
        Date originalDate = sdf.get2DigitYearStart();
        assertNotSame(sdf.get2DigitYearStart(), originalDate);
        assertEquals(sdf.get2DigitYearStart(), originalDate);
        originalDate.setTime(0);
        assertFalse(sdf.get2DigitYearStart().equals(originalDate));
        // Test that set2DigitYearStart takes a clone.
        Date newDate = new Date();
        sdf.set2DigitYearStart(newDate);
        assertNotSame(sdf.get2DigitYearStart(), newDate);
        assertEquals(sdf.get2DigitYearStart(), newDate);
        newDate.setTime(0);
        assertFalse(sdf.get2DigitYearStart().equals(newDate));
    }
}
