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

package libcore.xml;

import java.io.IOException;
import java.io.StringReader;
import junit.framework.TestCase;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Test doctype handling in pull parsers.
 */
public abstract class PullParserDtdTest extends TestCase {

    /**
     * Android's Expat pull parser permits parameter entities to be declared,
     * but it doesn't permit such entities to be used.
     */
    public void testDeclaringParameterEntities() throws Exception {
        String xml = "<!DOCTYPE foo ["
            + "  <!ENTITY % a \"android\">"
            + "]><foo></foo>";
        XmlPullParser parser = newPullParser(xml);
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
        }
    }

    public void testUsingParameterEntitiesInDtds() throws Exception {
        String xml = "<!DOCTYPE foo ["
            + "  <!ENTITY % a \"android\">"
            + "  <!ENTITY b \"%a;\">"
            + "]><foo></foo>";
        XmlPullParser parser = newPullParser(xml);
        assertParseFailure(parser);
    }

    public void testUsingParameterInDocuments() throws Exception {
        String xml = "<!DOCTYPE foo ["
            + "  <!ENTITY % a \"android\">"
            + "]><foo>&a;</foo>";
        XmlPullParser parser = newPullParser(xml);
        assertParseFailure(parser);
    }

    public void testInternalEntities() throws Exception {
        String xml = "<!DOCTYPE foo ["
                + "  <!ENTITY a \"android\">"
                + "]><foo>&a;</foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("android", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testExternalDtdIsSilentlyIgnored() throws Exception {
        String xml = "<!DOCTYPE foo SYSTEM \"http://127.0.0.1:1/no-such-file.dtd\"><foo></foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testExternalAndInternalDtd() throws Exception {
        String xml = "<!DOCTYPE foo SYSTEM \"http://127.0.0.1:1/no-such-file.dtd\" ["
                + "  <!ENTITY a \"android\">"
                + "]><foo>&a;</foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("android", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testInternalEntitiesAreParsed() throws Exception {
        String xml = "<!DOCTYPE foo ["
                + "  <!ENTITY a \"&#38;#65;\">" // &#38; expands to '&', &#65; expands to 'A'
                + "]><foo>&a;</foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("A", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testFoolishlyRecursiveInternalEntities() throws Exception {
        String xml = "<!DOCTYPE foo ["
                + "  <!ENTITY a \"&#38;#38;#38;#38;\">" // expand &#38; to '&' only twice
                + "]><foo>&a;</foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("&#38;#38;", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    /**
     * Android's Expat replaces external entities with the empty string.
     */
    public void testUsingExternalEntities() throws Exception {
        String xml = "<!DOCTYPE foo ["
                + "  <!ENTITY a SYSTEM \"http://localhost:1/no-such-file.xml\">"
                + "]><foo>&a;</foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        // &a; is dropped!
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testExternalIdIsCaseSensitive() throws Exception {
        // The spec requires 'SYSTEM' in upper case
        String xml = "<!DOCTYPE foo ["
                + "  <!ENTITY a system \"http://localhost:1/no-such-file.xml\">"
                + "]><foo/>";
        XmlPullParser parser = newPullParser(xml);
        assertParseFailure(parser);
    }

    /**
     * Use a DTD to specify that {@code <foo>} only contains {@code <bar>} tags.
     * Validating parsers react to this by dropping whitespace between the two
     * tags.
     */
    public void testDtdDoesNotInformIgnorableWhitespace() throws Exception {
        String xml = "<!DOCTYPE foo [\n"
                + "  <!ELEMENT foo (bar)*>\n"
                + "  <!ELEMENT bar ANY>\n"
                + "]>"
                + "<foo>  \n  <bar></bar>  \t  </foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("  \n  ", parser.getText());
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("bar", parser.getName());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals("bar", parser.getName());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("  \t  ", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testEmptyDoesNotInformIgnorableWhitespace() throws Exception {
        String xml = "<!DOCTYPE foo [\n"
                + "  <!ELEMENT foo EMPTY>\n"
                + "]>"
                + "<foo>  \n  </foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("  \n  ", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    /**
     * Test that the parser doesn't expand the entity attributes.
     */
    public void testAttributeOfTypeEntity() throws Exception {
        String xml = "<!DOCTYPE foo [\n"
                + "  <!ENTITY a \"android\">"
                + "  <!ELEMENT foo ANY>\n"
                + "  <!ATTLIST foo\n"
                + "    bar ENTITY #IMPLIED>"
                + "]>"
                + "<foo bar=\"a\"></foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals("a", parser.getAttributeValue(null, "bar"));
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testTagStructureNotValidated() throws Exception {
        String xml = "<!DOCTYPE foo [\n"
                + "  <!ELEMENT foo (bar)*>\n"
                + "  <!ELEMENT bar ANY>\n"
                + "  <!ELEMENT baz ANY>\n"
                + "]>"
                + "<foo><bar/><bar/><baz/></foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("bar", parser.getName());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("bar", parser.getName());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("baz", parser.getName());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testAttributeDefaultValues() throws Exception {
        String xml = "<!DOCTYPE foo [\n"
                + "  <!ELEMENT foo ANY>\n"
                + "  <!ATTLIST foo\n"
                + "    bar (a|b|c)  \"c\">"
                + "]>"
                + "<foo></foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals("c", parser.getAttributeValue(null, "bar"));
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testRequiredAttributesOmitted() throws Exception {
        String xml = "<!DOCTYPE foo [\n"
                + "  <!ELEMENT foo ANY>\n"
                + "  <!ATTLIST foo\n"
                + "    bar (a|b|c) #REQUIRED>"
                + "]>"
                + "<foo></foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(null, parser.getAttributeValue(null, "bar"));
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testFixedAttributesWithConflictingValues() throws Exception {
        String xml = "<!DOCTYPE foo [\n"
                + "  <!ELEMENT foo ANY>\n"
                + "  <!ATTLIST foo\n"
                + "    bar (a|b|c) #FIXED \"c\">"
                + "]>"
                + "<foo bar=\"a\"></foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals("a", parser.getAttributeValue(null, "bar"));
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testParsingNotations() throws Exception {
        String xml = "<!DOCTYPE foo [\n"
                + "  <!NOTATION type-a PUBLIC \"application/a\"> \n"
                + "  <!NOTATION type-b PUBLIC \"image/b\">\n"
                + "  <!NOTATION type-c PUBLIC \"-//W3C//DTD SVG 1.1//EN\"\n"
                + "     \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\"> \n"
                + "  <!ENTITY file          SYSTEM \"d.xml\">\n"
                + "  <!ENTITY fileWithNdata SYSTEM \"e.bin\" NDATA type-b>\n"
                + "]>"
                + "<foo type=\"type-a\"/>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testCommentsInDoctype() throws Exception {
        String xml = "<!DOCTYPE foo ["
                + "  <!-- ' -->"
                + "]><foo>android</foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("android", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testDoctypeNameOnly() throws Exception {
        String xml = "<!DOCTYPE foo><foo></foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testDoctypeWithNextToken() throws Exception {
        String xml = "<!DOCTYPE foo [<!ENTITY bb \"bar baz\">]><foo>a&bb;c</foo>";
        XmlPullParser parser = newPullParser(xml);
        assertEquals(XmlPullParser.DOCDECL, parser.nextToken());
        assertEquals(" foo [<!ENTITY bb \"bar baz\">]", parser.getText());
        assertNull(parser.getName());
        assertEquals(XmlPullParser.START_TAG, parser.nextToken());
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("a", parser.getText());
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken());
        assertEquals("bb", parser.getName());
        assertEquals("bar baz", parser.getText()); // TODO: this fails on gingerbread
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("c", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    private void assertParseFailure(XmlPullParser parser) throws IOException {
        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
            }
            fail();
        } catch (XmlPullParserException expected) {
        }
    }

    private XmlPullParser newPullParser(String xml) throws XmlPullParserException {
        XmlPullParser result = newPullParser();
        result.setInput(new StringReader(xml));
        return result;
    }

    /**
     * Creates a new pull parser.
     */
    abstract XmlPullParser newPullParser();
}
