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

package org.apache.harmony.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import junit.framework.TestCase;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class PullParserTest extends TestCase {
    private static final String SNIPPET = "<dagny dad=\"bob\">hello</dagny>";

    public void testPullParser() {
        try {
            XmlPullParser parser = newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);

            // Test reader.
            parser.setInput(new StringReader(SNIPPET));
            validate(parser);

            // Test input stream.
            parser.setInput(new ByteArrayInputStream(SNIPPET.getBytes()),
                    "UTF-8");
            validate(parser);
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void validate(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        assertEquals(XmlPullParser.START_DOCUMENT, parser.getEventType());

        assertEquals(0, parser.getDepth());

        assertEquals(XmlPullParser.START_TAG, parser.next());

        assertEquals(1, parser.getDepth());

        assertEquals("dagny", parser.getName());
        assertEquals(1, parser.getAttributeCount());
        assertEquals("dad", parser.getAttributeName(0));
        assertEquals("bob", parser.getAttributeValue(0));
        assertEquals("bob", parser.getAttributeValue(null, "dad"));

        assertEquals(XmlPullParser.TEXT, parser.next());

        assertEquals(1, parser.getDepth());

        assertEquals("hello", parser.getText());

        assertEquals(XmlPullParser.END_TAG, parser.next());

        assertEquals(1, parser.getDepth());

        assertEquals("dagny", parser.getName());

        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());

        assertEquals(0, parser.getDepth());
    }

    static final String XML =
        "<one xmlns='ns:default' xmlns:n1='ns:1' a='b'>\n"
              + "  <n1:two c='d' n1:e='f' xmlns:n2='ns:2'>text</n1:two>\n"
              + "</one>";

    public void testExpatPullParserNamespaces() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(XML));

        assertEquals(0, parser.getDepth());
        assertEquals(0, parser.getNamespaceCount(0));

        try {
            parser.getNamespaceCount(1);
            fail();
        } catch (IndexOutOfBoundsException e) { /* expected */ }

        // one
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(1, parser.getDepth());

        checkNamespacesInOne(parser);

        // n1:two
        assertEquals(XmlPullParser.START_TAG, parser.nextTag());

        assertEquals(2, parser.getDepth());
        checkNamespacesInTwo(parser);

        // Body of two.
        assertEquals(XmlPullParser.TEXT, parser.next());

        // End of two.
        assertEquals(XmlPullParser.END_TAG, parser.nextTag());

        // Depth should still be 2.
        assertEquals(2, parser.getDepth());

        // We should still be able to see the namespaces from two.
        checkNamespacesInTwo(parser);

        // End of one.
        assertEquals(XmlPullParser.END_TAG, parser.nextTag());

        // Depth should be back to 1.
        assertEquals(1, parser.getDepth());

        // We can still see the namespaces in one.
        checkNamespacesInOne(parser);

        // We shouldn't be able to see the namespaces in two anymore.
        try {
            parser.getNamespaceCount(2);
            fail();
        } catch (IndexOutOfBoundsException e) { /* expected */ }

        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());

        // We shouldn't be able to see the namespaces in one anymore.
        try {
            parser.getNamespaceCount(1);
            fail();
        } catch (IndexOutOfBoundsException e) { /* expected */ }

        assertEquals(0, parser.getNamespaceCount(0));
    }

    private void checkNamespacesInOne(XmlPullParser parser) throws XmlPullParserException {
        assertEquals(2, parser.getNamespaceCount(1));

        // Prefix for default namespace is null.
        assertNull(parser.getNamespacePrefix(0));
        assertEquals("ns:default", parser.getNamespaceUri(0));

        assertEquals("n1", parser.getNamespacePrefix(1));
        assertEquals("ns:1", parser.getNamespaceUri(1));

        assertEquals("ns:default", parser.getNamespace(null));

        // KXML returns null.
        // assertEquals("ns:default", parser.getNamespace(""));
    }

    private void checkNamespacesInTwo(XmlPullParser parser) throws XmlPullParserException {
        // These should still be valid.
        checkNamespacesInOne(parser);

        assertEquals(3, parser.getNamespaceCount(2));

        // Default ns should still be in the stack
        assertNull(parser.getNamespacePrefix(0));
        assertEquals("ns:default", parser.getNamespaceUri(0));
    }

    /**
     * Creates a new pull parser with namespace support.
     */
    abstract XmlPullParser newPullParser();
}
