/*
 * Copyright (C) 2011 The Android Open Source Project
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

package libcore.xml;

import java.io.StringWriter;
import junit.framework.TestCase;
import org.kxml2.io.KXmlSerializer;
import org.xmlpull.v1.XmlSerializer;

public final class KxmlSerializerTest extends TestCase {
    public void testWriteDocument() throws Exception {
        StringWriter stringWriter = new StringWriter();
        XmlSerializer serializer = new KXmlSerializer();
        serializer.setOutput(stringWriter);
        serializer.startDocument("UTF-8", null);
        serializer.startTag(null, "foo");
        serializer.attribute(null, "quux", "abc");
        serializer.startTag(null, "bar");
        serializer.endTag(null, "bar");
        serializer.startTag(null, "baz");
        serializer.endTag(null, "baz");
        serializer.endTag(null, "foo");
        serializer.endDocument();
        assertXmlEquals("<foo quux=\"abc\"><bar /><baz /></foo>", stringWriter.toString());
    }

    // http://code.google.com/p/android/issues/detail?id=21250
    public void testWriteSpecialCharactersInText() throws Exception {
        StringWriter stringWriter = new StringWriter();
        XmlSerializer serializer = new KXmlSerializer();
        serializer.setOutput(stringWriter);
        serializer.startDocument("UTF-8", null);
        serializer.startTag(null, "foo");
        serializer.text("5'8\", 5 < 6 & 7 > 3!");
        serializer.endTag(null, "foo");
        serializer.endDocument();
        assertXmlEquals("<foo>5'8\", 5 &lt; 6 &amp; 7 &gt; 3!</foo>", stringWriter.toString());
    }

    private void assertXmlEquals(String expectedXml, String actualXml) throws Exception {
        String declaration = "<?xml version='1.0' encoding='UTF-8' ?>";
        assertEquals(declaration + expectedXml, actualXml);
    }
}
