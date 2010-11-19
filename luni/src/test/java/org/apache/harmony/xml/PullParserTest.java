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

    public void testAttributeNoValueWithRelaxed() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true);
        parser.setInput(new StringReader("<input checked></input>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("input", parser.getName());
        assertEquals("checked", parser.getAttributeName(0));
        assertEquals("checked", parser.getAttributeValue(0));
    }

    public void testAttributeUnquotedValueWithRelaxed() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true);
        parser.setInput(new StringReader("<input checked=true></input>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("input", parser.getName());
        assertEquals("checked", parser.getAttributeName(0));
        assertEquals("true", parser.getAttributeValue(0));
    }

    public void testUnterminatedEntityWithRelaxed() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true);
        parser.setInput(new StringReader("<foo bar='A&W'>mac&cheese</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals("bar", parser.getAttributeName(0));
        assertEquals("A&W", parser.getAttributeValue(0));
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("mac&cheese", parser.getText());
    }

    public void testEntitiesAndNamespaces() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", true);
        parser.setInput(new StringReader(
                "<foo:a xmlns:foo='http://foo' xmlns:bar='http://bar'><bar:b/></foo:a>"));
        testNamespace(parser);
    }

    public void testEntitiesAndNamespacesWithRelaxed() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setFeature("http://xmlpull.org/v1/doc/features.html#process-namespaces", true);
        parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true);
        parser.setInput(new StringReader(
                "<foo:a xmlns:foo='http://foo' xmlns:bar='http://bar'><bar:b/></foo:a>"));
        testNamespace(parser); // TODO: end tag fails on gingerbread for relaxed mode
    }

    private void testNamespace(XmlPullParser parser) throws XmlPullParserException, IOException {
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("http://foo", parser.getNamespace());
        assertEquals("a", parser.getName());
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("http://bar", parser.getNamespace());
        assertEquals("b", parser.getName());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals("http://bar", parser.getNamespace());
        assertEquals("b", parser.getName());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals("http://foo", parser.getNamespace());
        assertEquals("a", parser.getName());
    }

    public void testNumericEntitiesLargerThanChar() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(
                "<foo>&#2147483647; &#-2147483648;</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertNextFails(parser);
    }

    public void testNumericEntitiesLargerThanInt() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(
                "<foo>&#2147483648;</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertNextFails(parser);
    }

    public void testCharacterReferenceOfHexUtf16Surrogates() throws Exception {
        testCharacterReferenceOfUtf16Surrogates("<foo>&#x10000; &#x10381; &#x10FFF0;</foo>");
    }

    public void testCharacterReferenceOfDecimalUtf16Surrogates() throws Exception {
        testCharacterReferenceOfUtf16Surrogates("<foo>&#65536; &#66433; &#1114096;</foo>");
    }

    private void testCharacterReferenceOfUtf16Surrogates(String xml) throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(xml));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals(new String(new int[] { 65536, ' ', 66433, ' ', 1114096 }, 0, 5),
                parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testCharacterReferenceOfLastUtf16Surrogate() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>&#x10FFFF;</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals(new String(new int[] { 0x10FFFF }, 0, 1), parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

        public void testOmittedNumericEntities() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>&#;</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertNextFails(parser);
    }

    /**
     * Carriage returns followed by line feeds are silently discarded.
     */
    public void testCarriageReturnLineFeed() throws Exception {
        testLineEndings("\r\n<foo\r\na='b\r\nc'\r\n>d\r\ne</foo\r\n>\r\n");
    }

    /**
     * Lone carriage returns are treated like newlines.
     */
    public void testLoneCarriageReturn() throws Exception {
        testLineEndings("\r<foo\ra='b\rc'\r>d\re</foo\r>\r");
    }

    public void testLoneNewLine() throws Exception {
        testLineEndings("\n<foo\na='b\nc'\n>d\ne</foo\n>\n");
    }

    private void testLineEndings(String xml) throws XmlPullParserException, IOException {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(xml));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals("b c", parser.getAttributeValue(0));
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("d\ne", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
        assertEquals("foo", parser.getName());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.next());
    }

    public void testXmlDeclaration() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(
                "<?xml version='1.0' encoding='UTF-8' standalone='no'?><foo/>"));
        assertEquals(XmlPullParser.START_TAG, parser.nextToken());
        assertEquals("1.0", parser.getProperty(
                "http://xmlpull.org/v1/doc/properties.html#xmldecl-version"));
        assertEquals(Boolean.FALSE, parser.getProperty(
                "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone"));
        assertEquals("UTF-8", parser.getInputEncoding());
    }

    public void testXmlDeclarationExtraAttributes() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(
                "<?xml version='1.0' encoding='UTF-8' standalone='no' a='b'?><foo/>"));
        try {
            parser.nextToken();
            fail();
        } catch (XmlPullParserException expected) {
        }
    }

    public void testCustomEntitiesUsingNext() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(
                "<foo a='cd&aaaaaaaaaa;ef'>wx&aaaaaaaaaa;yz</foo>"));
        parser.defineEntityReplacementText("aaaaaaaaaa", "b");
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("cdbef", parser.getAttributeValue(0));
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("wxbyz", parser.getText());
    }

    public void testCustomEntitiesUsingNextToken() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(
                "<foo a='cd&aaaaaaaaaa;ef'>wx&aaaaaaaaaa;yz</foo>"));
        parser.defineEntityReplacementText("aaaaaaaaaa", "b");
        assertEquals(XmlPullParser.START_TAG, parser.nextToken());
        assertEquals("cdbef", parser.getAttributeValue(0));
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("wx", parser.getText());
        assertEquals(XmlPullParser.ENTITY_REF, parser.nextToken());
        assertEquals("aaaaaaaaaa", parser.getName());
        assertEquals("b", parser.getText());
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("yz", parser.getText());
    }

    public void testGreaterThanInText() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals(">", parser.getText());
    }

    public void testGreaterThanInAttribute() throws Exception{
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo a='>'></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(">", parser.getAttributeValue(0));
    }

    public void testLessThanInText() throws Exception{
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo><</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertNextFails(parser);
    }

    public void testLessThanInAttribute() throws Exception{
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo a='<'></foo>"));
        assertNextFails(parser);
    }

    public void testQuotesInAttribute() throws Exception{
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo a='\"' b=\"'\"></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("\"", parser.getAttributeValue(0));
        assertEquals("'", parser.getAttributeValue(1));
    }

    public void testQuotesInText() throws Exception{
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>\" '</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("\" '", parser.getText());
    }

    public void testCdataDelimiterInAttribute() throws Exception{
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo a=']]>'></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals("]]>", parser.getAttributeValue(0));
    }

    public void testCdataDelimiterInText() throws Exception{
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>]]></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertNextFails(parser);
    }

    public void testUnexpectedEof() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo><![C"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertNextFails(parser);
    }

    public void testUnexpectedSequence() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo><![Cdata[bar]]></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertNextFails(parser);
    }

    public void testThreeDashCommentDelimiter() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo><!--a---></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertNextFails(parser);
    }

    public void testTwoDashesInComment() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo><!-- -- --></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertNextFails(parser);
    }

    public void testEmptyComment() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo><!----></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.COMMENT, parser.nextToken());
        assertEquals("", parser.getText());
    }

    /**
     * Close braces require lookaheads because we need to defend against "]]>".
     */
    public void testManyCloseBraces() throws Exception{
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>]]]]]]]]]]]]]]]]]]]]]]]</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("]]]]]]]]]]]]]]]]]]]]]]]", parser.getText());
    }

    public void testCommentWithNext() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>ab<!-- comment! -->cd</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("abcd", parser.getText());
    }

    public void testCommentWithNextToken() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>ab<!-- comment! -->cd</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("ab", parser.getText());
        assertEquals(XmlPullParser.COMMENT, parser.nextToken());
        assertEquals(" comment! ", parser.getText());
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("cd", parser.getText());
    }

    public void testCdataWithNext() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>ab<![CDATA[cdef]]gh&amp;i]]>jk</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("abcdef]]gh&amp;ijk", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testCdataWithNextToken() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>ab<![CDATA[cdef]]gh&amp;i]]>jk</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("ab", parser.getText());
        assertEquals(XmlPullParser.CDSECT, parser.nextToken());
        assertEquals("cdef]]gh&amp;i", parser.getText());
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("jk", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.nextToken());
    }

    public void testEntityLooksLikeCdataClose() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>&#93;&#93;></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("]]>", parser.getText());
    }

    public void testDoctypeWithNext() throws Exception {
        String s = "<!DOCTYPE foo ["
            + "  <!ENTITY bb \"bar baz\">"
            + "  <!NOTATION png SYSTEM \"image/png\">"
            + "]><foo>a&bb;c</foo>";
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(
                "<!DOCTYPE foo [<!ENTITY bb \"bar baz\">]><foo>a&bb;c</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("abar bazc", parser.getText()); // TODO: this fails on gingerbread
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testDoctypeWithNextToken() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader(
                "<!DOCTYPE foo [<!ENTITY bb \"bar baz\">]><foo>a&bb;c</foo>"));
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
    }

    public void testProcessingInstructionWithNext() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>ab<?cd efg hij?>kl</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.TEXT, parser.next());
        assertEquals("abkl", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testProcessingInstructionWithNextToken() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo>ab<?cd efg hij?>kl</foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.nextToken());
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("ab", parser.getText());
        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, parser.nextToken());
        assertEquals("cd efg hij", parser.getText());
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("kl", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testLinesAndColumns() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("\n"
                + "  <foo><bar a='\n"
                + "' b='cde'></bar\n"
                + "><!--\n"
                + "\n"
                + "--><baz/>fg\n"
                + "</foo>"));
        assertEquals("1,1", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, parser.nextToken());
        assertEquals("2,3", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.START_TAG, parser.nextToken());
        assertEquals("2,8", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.START_TAG, parser.nextToken());
        assertEquals("3,11", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.END_TAG, parser.nextToken());
        assertEquals("4,2", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.COMMENT, parser.nextToken());
        assertEquals("6,4", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.START_TAG, parser.nextToken());
        assertEquals("6,10", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.END_TAG, parser.nextToken());
        assertEquals("6,10", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.TEXT, parser.nextToken());
        assertEquals("7,1", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.END_TAG, parser.nextToken());
        assertEquals("7,7", parser.getLineNumber() + "," + parser.getColumnNumber());
        assertEquals(XmlPullParser.END_DOCUMENT, parser.nextToken());
        assertEquals("7,7", parser.getLineNumber() + "," + parser.getColumnNumber());
    }

    public void testEmptyCdataWithNext() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo><![CDATA[]]></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testEmptyCdataWithNextToken() throws Exception {
        XmlPullParser parser = newPullParser();
        parser.setInput(new StringReader("<foo><![CDATA[]]></foo>"));
        assertEquals(XmlPullParser.START_TAG, parser.next());
        assertEquals(XmlPullParser.CDSECT, parser.nextToken());
        assertEquals("", parser.getText());
        assertEquals(XmlPullParser.END_TAG, parser.next());
    }

    public void testParseReader() throws Exception {
        String snippet = "<dagny dad=\"bob\">hello</dagny>";
        XmlPullParser parser = newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new StringReader(snippet));
        validate(parser);
    }

    public void testParseInputStream() throws Exception {
        String snippet = "<dagny dad=\"bob\">hello</dagny>";
        XmlPullParser parser = newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new ByteArrayInputStream(snippet.getBytes()), "UTF-8");
        validate(parser);
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

    public void testNamespaces() throws Exception {
        String xml = "<one xmlns='ns:default' xmlns:n1='ns:1' a='b'>\n"
                + "  <n1:two c='d' n1:e='f' xmlns:n2='ns:2'>text</n1:two>\n"
                + "</one>";

        XmlPullParser parser = newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new StringReader(xml));

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

    private void assertNextFails(XmlPullParser parser) throws IOException {
        try {
            parser.next();
            fail();
        } catch (XmlPullParserException expected) {
        }
    }

    /**
     * Creates a new pull parser with namespace support.
     */
    abstract XmlPullParser newPullParser();
}
