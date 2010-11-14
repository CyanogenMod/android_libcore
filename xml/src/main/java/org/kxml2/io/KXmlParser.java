/* Copyright (c) 2002,2003, Stefan Haustein, Oberhausen, Rhld., Germany
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The  above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE. */

// Contributors: Paul Hackenberger (unterminated entity handling in relaxed mode)

package org.kxml2.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import libcore.internal.StringPool;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A pull based XML parser.
 */
public class KXmlParser implements XmlPullParser {

    private static final char[] START_COMMENT = { '<', '!', '-', '-' };
    private static final char[] END_COMMENT = { '-', '-', '>' };
    private static final char[] START_CDATA = { '<', '!', '[', 'C', 'D', 'A', 'T', 'A', '[' };
    private static final char[] END_CDATA = { ']', ']', '>' };
    private static final char[] START_PROCESSING_INSTRUCTION = { '<', '?' };
    private static final char[] END_PROCESSING_INSTRUCTION = { '?', '>' };
    private static final char[] START_DOCTYPE = { '<', '!', 'D', 'O', 'C', 'T', 'Y', 'P', 'E' };
    // no END_DOCTYPE because doctype must be parsed

    static final private String UNEXPECTED_EOF = "Unexpected EOF";
    static final private String ILLEGAL_TYPE = "Wrong event type";
    static final private int XML_DECLARATION = 998;

    // general
    private String location;

    private String version;
    private Boolean standalone;

    private boolean processNsp;
    private boolean relaxed;
    private boolean keepNamespaceAttributes;
    private Map<String, String> entityMap;
    private int depth;
    private String[] elementStack = new String[16];
    private String[] nspStack = new String[8];
    private int[] nspCounts = new int[4];

    // source

    private Reader reader;
    private String encoding;
    private final char[] buffer = new char[8192];
    private int position = 0;
    private int limit = 0;

    /*
     * Track the number of newlines and columns preceding the current buffer. To
     * compute the line and column of a position in the buffer, compute the line
     * and column in the buffer and add the preceding values.
     */
    private int bufferStartLine;
    private int bufferStartColumn;

    // the current token

    private int type;
    private boolean isWhitespace;
    private String namespace;
    private String prefix;
    private String name;
    private String text;

    private boolean degenerated;
    private int attributeCount;

    /*
     * The current element's attributes arranged in groups of 4:
     * i + 0 = attribute namespace URI
     * i + 1 = attribute namespace prefix
     * i + 2 = attribute qualified name (may contain ":", as in "html:h1")
     * i + 3 = attribute value
     */
    private String[] attributes = new String[16];

    private String error;

    private boolean unresolved;
    private boolean token;

    public final StringPool stringPool = new StringPool();

    /**
     * Retains namespace attributes like {@code xmlns="http://foo"} or {@code xmlns:foo="http:foo"}
     * in pulled elements. Most applications will only be interested in the effective namespaces of
     * their elements, so these attributes aren't useful. But for structure preserving wrappers like
     * DOM, it is necessary to keep the namespace data around.
     */
    public void keepNamespaceAttributes() {
        this.keepNamespaceAttributes = true;
    }

    private boolean isProp(String n1, boolean prop, String n2) {
        if (!n1.startsWith("http://xmlpull.org/v1/doc/")) {
            return false;
        }
        if (prop) {
            return n1.substring(42).equals(n2);
        } else {
            return n1.substring(40).equals(n2);
        }
    }

    private boolean adjustNsp() throws XmlPullParserException {
        boolean any = false;

        for (int i = 0; i < attributeCount << 2; i += 4) {
            String attrName = attributes[i + 2];
            int cut = attrName.indexOf(':');
            String prefix;

            if (cut != -1) {
                prefix = attrName.substring(0, cut);
                attrName = attrName.substring(cut + 1);
            } else if (attrName.equals("xmlns")) {
                prefix = attrName;
                attrName = null;
            } else {
                continue;
            }

            if (!prefix.equals("xmlns")) {
                any = true;
            } else {
                int j = (nspCounts[depth]++) << 1;

                nspStack = ensureCapacity(nspStack, j + 2);
                nspStack[j] = attrName;
                nspStack[j + 1] = attributes[i + 3];

                if (attrName != null && attributes[i + 3].isEmpty()) {
                    checkRelaxed("illegal empty namespace");
                }

                if (keepNamespaceAttributes) {
                    // explicitly set the namespace for unprefixed attributes
                    // such as xmlns="http://foo"
                    attributes[i] = "http://www.w3.org/2000/xmlns/";
                    any = true;
                } else {
                    System.arraycopy(
                            attributes,
                            i + 4,
                            attributes,
                            i,
                            ((--attributeCount) << 2) - i);

                    i -= 4;
                }
            }
        }

        if (any) {
            for (int i = (attributeCount << 2) - 4; i >= 0; i -= 4) {

                String attrName = attributes[i + 2];
                int cut = attrName.indexOf(':');

                if (cut == 0 && !relaxed) {
                    throw new RuntimeException(
                            "illegal attribute name: " + attrName + " at " + this);
                } else if (cut != -1) {
                    String attrPrefix = attrName.substring(0, cut);

                    attrName = attrName.substring(cut + 1);

                    String attrNs = getNamespace(attrPrefix);

                    if (attrNs == null && !relaxed) {
                        throw new RuntimeException(
                                "Undefined Prefix: " + attrPrefix + " in " + this);
                    }

                    attributes[i] = attrNs;
                    attributes[i + 1] = attrPrefix;
                    attributes[i + 2] = attrName;
                }
            }
        }

        int cut = name.indexOf(':');

        if (cut == 0) {
            checkRelaxed("illegal tag name: " + name);
        }

        if (cut != -1) {
            prefix = name.substring(0, cut);
            name = name.substring(cut + 1);
        }

        this.namespace = getNamespace(prefix);

        if (this.namespace == null) {
            if (prefix != null) {
                checkRelaxed("undefined prefix: " + prefix);
            }
            this.namespace = NO_NAMESPACE;
        }

        return any;
    }

    private String[] ensureCapacity(String[] arr, int required) {
        if (arr.length >= required) {
            return arr;
        }
        String[] bigger = new String[required + 16];
        System.arraycopy(arr, 0, bigger, 0, arr.length);
        return bigger;
    }

    private void checkRelaxed(String errorMessage) throws XmlPullParserException {
        if (!relaxed) {
            throw new XmlPullParserException(errorMessage, this, null);
        }
        if (error == null) {
            error = "Error: " + errorMessage;
        }
    }

    /**
     * Common base for next() and nextToken(). Clears the state, except from txtPos and whitespace.
     * Does not set the type variable.
     */
    private void nextImpl() throws IOException, XmlPullParserException {
        if (reader == null) {
            throw new XmlPullParserException("setInput() must be called first.", this, null);
        }

        if (type == END_TAG) {
            depth--;
        }

        while (true) {
            attributeCount = -1;

            // degenerated needs to be handled before error because of possible
            // processor expectations(!)

            if (degenerated) {
                degenerated = false;
                type = END_TAG;
                return;
            }

            if (error != null) {
                text = error;
                error = null;
                type = COMMENT;
                return;
            }

            prefix = null;
            name = null;
            namespace = null;

            type = peekType();

            switch (type) {

            case ENTITY_REF:
                if (token) {
                    StringBuilder entityTextBuilder = new StringBuilder();
                    readEntity(entityTextBuilder);
                    text = entityTextBuilder.toString();
                    return;
                }
                // fall-through
            case TEXT:
                text = readValue('<', !token, false);
                if (depth == 0 && isWhitespace) {
                    type = IGNORABLE_WHITESPACE;
                }
                return;

            case START_TAG:
                text = null; // TODO: fix next()/nextToken() so this is handled there
                parseStartTag(false);
                return;

            case END_TAG:
                readEndTag();
                return;

            case END_DOCUMENT:
                return;

            case XML_DECLARATION:
                readXmlDeclaration();
                continue;

            case PROCESSING_INSTRUCTION:
                read(START_PROCESSING_INSTRUCTION);
                if (token) {
                    text = readUntil(END_PROCESSING_INSTRUCTION, true);
                } else {
                    readUntil(END_PROCESSING_INSTRUCTION, false);
                }
                return;

            case DOCDECL:
                readDoctype(token);
                return;

            case CDSECT:
                String oldText = text;
                read(START_CDATA);
                text = readUntil(END_CDATA, true);
                if (oldText != null) {
                    text = oldText + text; // TODO: fix next()/nextToken() so this is handled there
                }
                return;

            case COMMENT:
                read(START_COMMENT);
                if (token) {
                    text = readUntil(END_COMMENT, true);
                } else {
                    readUntil(END_COMMENT, false);
                }
                return;
            }
        }
    }

    /**
     * Reads text until the specified delimiter is encountered. Consumes the
     * text and the delimiter.
     *
     * @param returnText true to return the read text excluding the delimiter;
     *     false to return null.
     */
    private String readUntil(char[] delimiter, boolean returnText)
            throws IOException, XmlPullParserException {
        int previous = -1;
        int start = position;
        StringBuilder result = null;

        search:
        while (true) {
            if (position + delimiter.length >= limit) {
                if (start < position && returnText) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(buffer, start, position - start);
                }
                if (!fillBuffer(delimiter.length)) {
                    checkRelaxed(UNEXPECTED_EOF);
                    type = COMMENT;
                    return null;
                }
                start = position;
            }

            // TODO: replace with Arrays.equals(buffer, position, delimiter, 0, delimiter.length)
            // when the VM has better method inlining
            for (int i = 0; i < delimiter.length; i++) {
                if (buffer[position + i] != delimiter[i]) {
                    previous = buffer[position];
                    position++;
                    continue search;
                }
            }

            break;
        }

        if (delimiter == END_COMMENT && previous == '-') {
            checkRelaxed("illegal comment delimiter: --->");
        }

        int end = position;
        position += delimiter.length;

        if (!returnText) {
            return null;
        } else if (result == null) {
            return stringPool.get(buffer, start, end - start);
        } else {
            result.append(buffer, start, end - start);
            return result.toString();
        }
    }

    /**
     * Returns true if an XML declaration was read.
     */
    private boolean readXmlDeclaration() throws IOException, XmlPullParserException {
        if (bufferStartLine != 0 || bufferStartColumn != 0 || position != 0) {
            checkRelaxed("processing instructions must not start with xml");
        }

        read(START_PROCESSING_INSTRUCTION);
        parseStartTag(true);

        if (attributeCount < 1 || !"version".equals(attributes[2])) {
            checkRelaxed("version expected");
        }

        version = attributes[3];

        int pos = 1;

        if (pos < attributeCount && "encoding".equals(attributes[2 + 4])) {
            encoding = attributes[3 + 4];
            pos++;
        }

        if (pos < attributeCount && "standalone".equals(attributes[4 * pos + 2])) {
            String st = attributes[3 + 4 * pos];
            if ("yes".equals(st)) {
                standalone = Boolean.TRUE;
            } else if ("no".equals(st)) {
                standalone = Boolean.FALSE;
            } else {
                checkRelaxed("illegal standalone value: " + st);
            }
            pos++;
        }

        if (pos != attributeCount) {
            checkRelaxed("unexpected attributes in XML declaration");
        }

        isWhitespace = true;
        text = null;
        return true;
    }

    private void readDoctype(boolean assignText) throws IOException, XmlPullParserException {
        read(START_DOCTYPE);

        int start = position;
        StringBuilder result = null;
        int nesting = 1;
        boolean quoted = false;

        while (true) {
            if (position >= limit) {
                if (start < position && assignText) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(buffer, start, position - start);
                }
                if (!fillBuffer(1)) {
                    checkRelaxed(UNEXPECTED_EOF);
                    return;
                }
                start = position;
            }

            char i = buffer[position++];

            if (i == '\'') {
                quoted = !quoted; // TODO: should this include a double quote as well?
            } else if (i == '<') {
                if (!quoted) {
                    nesting++;
                }
            } else if (i == '>') {
                if (!quoted && --nesting == 0) {
                    break;
                }
            }
        }

        if (assignText) {
            if (result == null) {
                text = stringPool.get(buffer, start, position - start - 1); // omit the '>'
            } else {
                result.append(buffer, start, position - start - 1); // omit the '>'
                text = result.toString();
            }
        }
    }

    private void readEndTag() throws IOException, XmlPullParserException {
        read('<');
        read('/');
        name = readName(); // TODO: pass the expected name in as a hint?
        skip();
        read('>');

        int sp = (depth - 1) * 4;

        if (depth == 0) {
            checkRelaxed("read end tag " + name + " with no tags open");
            type = COMMENT;
            return;
        }

        if (!relaxed) {
            if (!name.equals(elementStack[sp + 3])) {
                throw new XmlPullParserException(
                        "expected: /" + elementStack[sp + 3] + " read: " + name, this, null);
            }

            namespace = elementStack[sp];
            prefix = elementStack[sp + 1];
            name = elementStack[sp + 2];
        }
    }

    /**
     * Returns the type of the next token.
     */
    private int peekType() throws IOException, XmlPullParserException {
        if (position >= limit && !fillBuffer(1)) {
            return END_DOCUMENT;
        }

        if (buffer[position] == '&') {
            return ENTITY_REF;

        } else if (buffer[position] == '<') {
            if (position + 2 >= limit && !fillBuffer(3)) {
                throw new XmlPullParserException("Dangling <", this, null);
            }

            if (buffer[position + 1] == '/') {
                return END_TAG;
            } else if (buffer[position + 1] == '?') {
                // we're looking for "<?xml " with case insensitivity
                if ((position + 5 < limit || fillBuffer(6))
                        && (buffer[position + 2] == 'x' || buffer[position + 2] == 'X')
                        && (buffer[position + 3] == 'm' || buffer[position + 3] == 'M')
                        && (buffer[position + 4] == 'l' || buffer[position + 4] == 'L')
                        && (buffer[position + 5] == ' ')) {
                    return XML_DECLARATION;
                } else {
                    return PROCESSING_INSTRUCTION;
                }
            } else if (buffer[position + 1] == '!') {
                if (buffer[position + 2] == START_DOCTYPE[2]) {
                    return DOCDECL;
                } else if (buffer[position + 2] == START_CDATA[2]) {
                    return CDSECT;
                } else if (buffer[position + 2] == START_COMMENT[2]) {
                    return COMMENT;
                } else {
                    throw new XmlPullParserException("Unexpected <!", this, null);
                }
            } else {
                return START_TAG;
            }
        } else {
            return TEXT;
        }
    }

    /**
     * Sets name and attributes
     */
    private void parseStartTag(boolean xmldecl) throws IOException, XmlPullParserException {
        if (!xmldecl) {
            read('<');
        }
        name = readName();
        attributeCount = 0;

        while (true) {
            skip();

            if (position >= limit && !fillBuffer(1)) {
                checkRelaxed(UNEXPECTED_EOF);
                return;
            }

            int c = buffer[position];

            if (xmldecl) {
                if (c == '?') {
                    position++;
                    read('>');
                    return;
                }
            } else {
                if (c == '/') {
                    degenerated = true;
                    position++;
                    skip();
                    read('>');
                    break;
                } else if (c == '>') {
                    position++;
                    break;
                }
            }

            String attrName = readName();

            int i = (attributeCount++) * 4;
            attributes = ensureCapacity(attributes, i + 4);
            attributes[i++] = "";
            attributes[i++] = null;
            attributes[i++] = attrName;

            skip();
            if (position >= limit && !fillBuffer(1)) {
                checkRelaxed(UNEXPECTED_EOF);
                return;
            }

            if (buffer[position] == '=') {
                position++;

                skip();
                if (position >= limit && !fillBuffer(1)) {
                    checkRelaxed(UNEXPECTED_EOF);
                    return;
                }
                char delimiter = buffer[position];

                if (delimiter == '\'' || delimiter == '"') {
                    position++;
                } else if (relaxed) {
                    delimiter = ' ';
                } else {
                    throw new XmlPullParserException("attr value delimiter missing!", this, null);
                }

                attributes[i] = readValue(delimiter, true, true);

                if (delimiter != ' ') {
                    position++; // end quote
                }
            } else if (relaxed) {
                attributes[i] = attrName;
            } else {
                checkRelaxed("Attr.value missing f. " + attrName);
                attributes[i] = attrName;
            }
        }

        int sp = depth++ * 4;
        elementStack = ensureCapacity(elementStack, sp + 4);
        elementStack[sp + 3] = name;

        if (depth >= nspCounts.length) {
            int[] bigger = new int[depth + 4];
            System.arraycopy(nspCounts, 0, bigger, 0, nspCounts.length);
            nspCounts = bigger;
        }

        nspCounts[depth] = nspCounts[depth - 1];

        if (processNsp) {
            adjustNsp();
        } else {
            namespace = "";
        }

        elementStack[sp] = namespace;
        elementStack[sp + 1] = prefix;
        elementStack[sp + 2] = name;
    }

    /**
     * Reads an entity reference from the buffer, resolves it, and writes the
     * resolved entity to {@code out}. If the entity cannot be read or resolved,
     * {@code out} will contain the partial entity reference.
     */
    private void readEntity(StringBuilder out) throws IOException, XmlPullParserException {
        int start = out.length();

        if (buffer[position++] != '&') {
            throw new AssertionError();
        }

        out.append('&');

        while (true) {
            int c = peekCharacter();

            if (c == ';') {
                position++;
                break;

            } else if (c >= 128
                    || (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || c == '_'
                    || c == '-'
                    || c == '#') {
                position++;
                out.append((char) c);

            } else if (relaxed) {
                // intentionally leave the partial reference in 'out'
                return;

            } else {
                throw new XmlPullParserException("unterminated entity ref", this, null);
            }
        }

        String code = out.substring(start + 1);
        out.delete(start, out.length());

        if (token && type == ENTITY_REF) {
            name = code;
        }

        if (code.charAt(0) == '#') {
            // TODO: check IndexOutOfBoundsException?
            // TODO: save an intermediate string for 'code' if unneeded?
            int c = code.charAt(1) == 'x'
                    ? Integer.parseInt(code.substring(2), 16)
                    : Integer.parseInt(code.substring(1));
            // TODO: set unresolved to false?
            out.append((char) c);
            return;
        }

        String resolved = entityMap.get(code);
        if (resolved != null) {
            unresolved = false;
            out.append(resolved);
            return;
        }

        unresolved = true;
        if (!token) {
            checkRelaxed("unresolved: &" + code + ";");
            // TODO: should the &code; show up in the text in relaxed mode?
        }
    }

    /**
     * Returns the current text or attribute value. This also has the side
     * effect of setting isWhitespace to false if a non-whitespace character is
     * encountered.
     *
     * @param delimiter {@code >} for text, {@code "} and {@code '} for quoted
     *     attributes, or a space for unquoted attributes.
     */
    private String readValue(char delimiter, boolean resolveEntities,
            boolean inAttributeValue) throws IOException, XmlPullParserException {

        /*
         * This method returns all of the characters from the current position
         * through to an appropriate delimiter.
         *
         * If we're lucky (which we usually are), we'll return a single slice of
         * the buffer. This fast path avoids allocating a string builder.
         *
         * There are 5 unlucky characters we could encounter:
         *  - "&":  entities must be resolved.
         *  - "<":  this isn't permitted in attributes unless relaxed.
         *  - "]":  this requires a lookahead to defend against the forbidden
         *          CDATA section delimiter "]]>".
         *  - "\r": If a "\r" is followed by a "\n", we discard the "\r". If it
         *          isn't followed by "\n", we replace "\r" with either a "\n"
         *          in text nodes or a space in attribute values.
         *  - "\n": In attribute values, "\n" must be replaced with a space.
         *
         * We could also get unlucky by needing to refill the buffer midway
         * through the text.
         */

        int start = position;
        StringBuilder result = null;

        // if a text section was already started, prefix the start
        if (text != null) {
            result = new StringBuilder();
            result.append(text);
        }

        while (true) {

            /*
             * Make sure we have at least a single character to read from the
             * buffer. This mutates the buffer, so save the partial result
             * to the slow path string builder first.
             */
            if (position >= limit) {
                if (start < position) {
                    if (result == null) {
                        result = new StringBuilder();
                    }
                    result.append(buffer, start, position - start);
                }
                if (!fillBuffer(1)) {
                    return result != null ? result.toString() : "";
                }
                start = position;
            }

            char c = buffer[position];

            if (c == delimiter
                    || (delimiter == ' ' && (c <= ' ' || c == '>'))
                    || c == '&' && !resolveEntities) {
                break;
            }

            if (c != '\r'
                    && (c != '\n' || !inAttributeValue)
                    && c != '&'
                    && c != '<'
                    && (c != ']' || inAttributeValue)) {
                isWhitespace &= (c <= ' ');
                position++;
                continue;
            }

            /*
             * We've encountered an unlucky character! Convert from fast
             * path to slow path if we haven't done so already.
             */
            if (result == null) {
                result = new StringBuilder();
            }
            result.append(buffer, start, position - start);

            if (c == '\r') {
                if ((position + 1 < limit || fillBuffer(2)) && buffer[position + 1] == '\n') {
                    position++;
                }
                c = inAttributeValue ? ' ' : '\n';

            } else if (c == '\n') {
                c = ' ';

            } else if (c == '&') {
                isWhitespace = false; // TODO: what if the entity resolves to whitespace?
                readEntity(result);
                start = position;
                continue;

            } else if (c == '<') {
                if (inAttributeValue) {
                    checkRelaxed("Illegal: \"<\" inside attribute value");
                }
                isWhitespace = false;

            } else if (c == ']') {
                if ((position + 2 < limit || fillBuffer(3))
                        && buffer[position + 1] == ']' && buffer[position + 2] == '>') {
                    checkRelaxed("Illegal: \"]]>\" outside CDATA section");
                }
                isWhitespace = false;

            } else {
                throw new AssertionError();
            }

            position++;
            result.append(c);
            start = position;
        }

        if (result == null) {
            return stringPool.get(buffer, start, position - start);
        } else {
            result.append(buffer, start, position - start);
            return result.toString();
        }
    }

    private void read(char expected) throws IOException, XmlPullParserException {
        int c = peekCharacter();
        if (c != expected) {
            checkRelaxed("expected: '" + expected + "' actual: '" + ((char) c) + "'");
        }
        position++;
    }

    private void read(char[] chars) throws IOException, XmlPullParserException {
        if (position + chars.length >= limit && !fillBuffer(chars.length)) {
            checkRelaxed("expected: '" + new String(chars) + "' but was EOF");
            return;
        }

        // TODO: replace with Arrays.equals(buffer, position, delimiter, 0, delimiter.length)
        // when the VM has better method inlining
        for (int i = 0; i < chars.length; i++) {
            if (buffer[position + i] != chars[i]) {
                checkRelaxed("expected: \"" + new String(chars) + "\" but was \""
                        + new String(buffer, position, chars.length) + "...\"");
            }
        }

        position += chars.length;
    }

    private int peekCharacter() throws IOException, XmlPullParserException {
        if (position < limit || fillBuffer(1)) {
            return buffer[position];
        }
        return -1;
    }

    /**
     * Returns true once {@code limit - position >= minimum}. If the data is
     * exhausted before that many characters are available, this returns
     * false.
     */
    private boolean fillBuffer(int minimum) throws IOException {
        // Before clobbering the old characters, update where buffer starts
        for (int i = 0; i < position; i++) {
            if (buffer[i] == '\n') {
                bufferStartLine++;
                bufferStartColumn = 0;
            } else {
                bufferStartColumn++;
            }
        }

        if (limit != position) {
            limit -= position;
            System.arraycopy(buffer, position, buffer, 0, limit);
        } else {
            limit = 0;
        }

        position = 0;
        int total;
        while ((total = reader.read(buffer, limit, buffer.length - limit)) != -1) {
            limit += total;
            if (limit >= minimum) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an element or attribute name. This is always non-empty for
     * non-relaxed parsers.
     */
    private String readName() throws IOException, XmlPullParserException {
        if (position >= limit && !fillBuffer(1)) {
            checkRelaxed("name expected");
            return "";
        }

        int start = position;
        StringBuilder result = null;

        // read the first character
        char c = buffer[position];
        if ((c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || c == '_'
                || c == ':'
                || c >= '\u00c0' // TODO: check the XML spec
                || relaxed) {
            position++;
        } else {
            checkRelaxed("name expected");
            return "";
        }

        while (true) {
            /*
             * Make sure we have at least a single character to read from the
             * buffer. This mutates the buffer, so save the partial result
             * to the slow path string builder first.
             */
            if (position >= limit) {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append(buffer, start, position - start);
                if (!fillBuffer(1)) {
                    return result.toString();
                }
                start = position;
            }

            // read another character
            c = buffer[position];
            if ((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '_'
                    || c == '-'
                    || c == ':'
                    || c == '.'
                    || c >= '\u00b7') {  // TODO: check the XML spec
                position++;
                continue;
            }

            // we encountered a non-name character. done!
            if (result == null) {
                return stringPool.get(buffer, start, position - start);
            } else {
                result.append(buffer, start, position - start);
                return result.toString();
            }
        }
    }

    private void skip() throws IOException {
        while (position < limit || fillBuffer(1)) {
            int c = buffer[position];
            if (c > ' ') {
                break;
            }
            position++;
        }
    }

    //  public part starts here...

    public void setInput(Reader reader) throws XmlPullParserException {
        this.reader = reader;

        type = START_DOCUMENT;
        name = null;
        namespace = null;
        degenerated = false;
        attributeCount = -1;
        encoding = null;
        version = null;
        standalone = null;

        if (reader == null) {
            return;
        }

        position = 0;
        limit = 0;
        bufferStartLine = 0;
        bufferStartColumn = 0;
        depth = 0;

        entityMap = new HashMap<String, String>();
        entityMap.put("amp", "&");
        entityMap.put("apos", "'");
        entityMap.put("gt", ">");
        entityMap.put("lt", "<");
        entityMap.put("quot", "\"");
    }

    public void setInput(InputStream is, String _enc) throws XmlPullParserException {
        position = 0;
        limit = 0;
        String enc = _enc;

        if (is == null) {
            throw new IllegalArgumentException();
        }

        try {
            if (enc == null) {
                // read the four bytes looking for an indication of the encoding in use
                int firstFourBytes = 0;
                while (limit < 4) {
                    int i = is.read();
                    if (i == -1) {
                        break;
                    }
                    firstFourBytes = (firstFourBytes << 8) | i;
                    buffer[limit++] = (char) i;
                }

                if (limit == 4) {
                    switch (firstFourBytes) {
                        case 0x00000FEFF: // UTF-32BE BOM
                            enc = "UTF-32BE";
                            limit = 0;
                            break;

                        case 0x0FFFE0000: // UTF-32LE BOM
                            enc = "UTF-32LE";
                            limit = 0;
                            break;

                        case 0x0000003c: // '>' in UTF-32BE
                            enc = "UTF-32BE";
                            buffer[0] = '<';
                            limit = 1;
                            break;

                        case 0x03c000000: // '<' in UTF-32LE
                            enc = "UTF-32LE";
                            buffer[0] = '<';
                            limit = 1;
                            break;

                        case 0x0003c003f: // "<?" in UTF-16BE
                            enc = "UTF-16BE";
                            buffer[0] = '<';
                            buffer[1] = '?';
                            limit = 2;
                            break;

                        case 0x03c003f00: // "<?" in UTF-16LE
                            enc = "UTF-16LE";
                            buffer[0] = '<';
                            buffer[1] = '?';
                            limit = 2;
                            break;

                        case 0x03c3f786d: // "<?xm" in ASCII etc.
                            while (true) {
                                int i = is.read();
                                if (i == -1) {
                                    break;
                                }
                                buffer[limit++] = (char) i;
                                if (i == '>') {
                                    String s = new String(buffer, 0, limit);
                                    int i0 = s.indexOf("encoding");
                                    if (i0 != -1) {
                                        while (s.charAt(i0) != '"'
                                                && s.charAt(i0) != '\'') {
                                            i0++;
                                        }
                                        char deli = s.charAt(i0++);
                                        int i1 = s.indexOf(deli, i0);
                                        enc = s.substring(i0, i1);
                                    }
                                    break;
                                }
                            }

                        default:
                            // handle a byte order mark followed by something other than <?
                            if ((firstFourBytes & 0x0ffff0000) == 0x0FEFF0000) {
                                enc = "UTF-16BE";
                                buffer[0] = (char) ((buffer[2] << 8) | buffer[3]);
                                limit = 1;
                            } else if ((firstFourBytes & 0x0ffff0000) == 0x0fffe0000) {
                                enc = "UTF-16LE";
                                buffer[0] = (char) ((buffer[3] << 8) | buffer[2]);
                                limit = 1;
                            } else if ((firstFourBytes & 0x0ffffff00) == 0x0EFBBBF00) {
                                enc = "UTF-8";
                                buffer[0] = buffer[3];
                                limit = 1;
                            }
                    }
                }
            }

            if (enc == null) {
                enc = "UTF-8";
            }

            int sc = limit;
            setInput(new InputStreamReader(is, enc));
            encoding = _enc;
            limit = sc;
        } catch (Exception e) {
            throw new XmlPullParserException("Invalid stream or encoding: " + e, this, e);
        }
    }

    public boolean getFeature(String feature) {
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(feature)) {
            return processNsp;
        } else if (isProp(feature, false, "relaxed")) {
            return relaxed;
        } else {
            return false;
        }
    }

    public String getInputEncoding() {
        return encoding;
    }

    public void defineEntityReplacementText(String entity, String value)
            throws XmlPullParserException {
        if (entityMap == null) {
            throw new RuntimeException("entity replacement text must be defined after setInput!");
        }
        entityMap.put(entity, value);
    }

    public Object getProperty(String property) {
        if (isProp(property, true, "xmldecl-version")) {
            return version;
        }
        if (isProp(property, true, "xmldecl-standalone")) {
            return standalone;
        }
        if (isProp(property, true, "location")) {
            return location != null ? location : reader.toString();
        }
        return null;
    }

    public int getNamespaceCount(int depth) {
        if (depth > this.depth) {
            throw new IndexOutOfBoundsException();
        }
        return nspCounts[depth];
    }

    public String getNamespacePrefix(int pos) {
        return nspStack[pos * 2];
    }

    public String getNamespaceUri(int pos) {
        return nspStack[(pos * 2) + 1];
    }

    public String getNamespace(String prefix) {

        if ("xml".equals(prefix)) {
            return "http://www.w3.org/XML/1998/namespace";
        }
        if ("xmlns".equals(prefix)) {
            return "http://www.w3.org/2000/xmlns/";
        }

        for (int i = (getNamespaceCount(depth) << 1) - 2; i >= 0; i -= 2) {
            if (prefix == null) {
                if (nspStack[i] == null) {
                    return nspStack[i + 1];
                }
            } else if (prefix.equals(nspStack[i])) {
                return nspStack[i + 1];
            }
        }
        return null;
    }

    public int getDepth() {
        return depth;
    }

    public String getPositionDescription() {
        StringBuilder buf = new StringBuilder(type < TYPES.length ? TYPES[type] : "unknown");
        buf.append(' ');

        if (type == START_TAG || type == END_TAG) {
            if (degenerated) {
                buf.append("(empty) ");
            }
            buf.append('<');
            if (type == END_TAG) {
                buf.append('/');
            }

            if (prefix != null) {
                buf.append("{" + namespace + "}" + prefix + ":");
            }
            buf.append(name);

            int cnt = attributeCount * 4;
            for (int i = 0; i < cnt; i += 4) {
                buf.append(' ');
                if (attributes[i + 1] != null) {
                    buf.append("{" + attributes[i] + "}" + attributes[i + 1] + ":");
                }
                buf.append(attributes[i + 2] + "='" + attributes[i + 3] + "'");
            }

            buf.append('>');
        } else if (type == IGNORABLE_WHITESPACE) {
            ;
        } else if (type != TEXT) {
            buf.append(getText());
        } else if (isWhitespace) {
            buf.append("(whitespace)");
        } else {
            String text = getText();
            if (text.length() > 16) {
                text = text.substring(0, 16) + "...";
            }
            buf.append(text);
        }

        buf.append("@" + getLineNumber() + ":" + getColumnNumber());
        if (location != null) {
            buf.append(" in ");
            buf.append(location);
        } else if (reader != null) {
            buf.append(" in ");
            buf.append(reader.toString());
        }
        return buf.toString();
    }

    public int getLineNumber() {
        int result = bufferStartLine;
        for (int i = 0; i < position; i++) {
            if (buffer[i] == '\n') {
                result++;
            }
        }
        return result + 1; // the first line is '1'
    }

    public int getColumnNumber() {
        int result = bufferStartColumn;
        for (int i = 0; i < position; i++) {
            if (buffer[i] == '\n') {
                result = 0;
            } else {
                result++;
            }
        }
        return result + 1; // the first column is '1'
    }

    public boolean isWhitespace() throws XmlPullParserException {
        if (type != TEXT && type != IGNORABLE_WHITESPACE && type != CDSECT) {
            throw new XmlPullParserException(ILLEGAL_TYPE, this, null);
        }
        return isWhitespace;
    }

    public String getText() {
        if (type < TEXT || (type == ENTITY_REF && unresolved)) {
            return null;
        } else if (text == null) {
            return "";
        } else {
            return text;
        }
    }

    public char[] getTextCharacters(int[] poslen) {
        String text = getText();
        if (text == null) {
            poslen[0] = -1;
            poslen[1] = -1;
            return null;
        }
        char[] result = text.toCharArray();
        poslen[0] = 0;
        poslen[1] = result.length;
        return result;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isEmptyElementTag() throws XmlPullParserException {
        if (type != START_TAG) {
            throw new XmlPullParserException(ILLEGAL_TYPE, this, null);
        }
        return degenerated;
    }

    public int getAttributeCount() {
        return attributeCount;
    }

    public String getAttributeType(int index) {
        return "CDATA";
    }

    public boolean isAttributeDefault(int index) {
        return false;
    }

    public String getAttributeNamespace(int index) {
        if (index >= attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return attributes[index * 4];
    }

    public String getAttributeName(int index) {
        if (index >= attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return attributes[(index * 4) + 2];
    }

    public String getAttributePrefix(int index) {
        if (index >= attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return attributes[(index * 4) + 1];
    }

    public String getAttributeValue(int index) {
        if (index >= attributeCount) {
            throw new IndexOutOfBoundsException();
        }
        return attributes[(index * 4) + 3];
    }

    public String getAttributeValue(String namespace, String name) {
        for (int i = (attributeCount * 4) - 4; i >= 0; i -= 4) {
            if (attributes[i + 2].equals(name)
                    && (namespace == null || attributes[i].equals(namespace))) {
                return attributes[i + 3];
            }
        }

        return null;
    }

    public int getEventType() throws XmlPullParserException {
        return type;
    }

    public int next() throws XmlPullParserException, IOException {
        text = null;
        isWhitespace = true;
        int minType = 9999;
        token = false;

        do {
            nextImpl();
            if (type < minType) {
                minType = type;
            }
        } while (minType > ENTITY_REF // ignorable
                || (minType >= TEXT && peekType() >= TEXT));

        type = minType;
        if (type > TEXT) {
            type = TEXT;
        }

        return type;
    }

    public int nextToken() throws XmlPullParserException, IOException {
        isWhitespace = true;
        text = null;

        token = true;
        nextImpl();
        return type;
    }

    // utility methods to make XML parsing easier ...

    public int nextTag() throws XmlPullParserException, IOException {
        next();
        if (type == TEXT && isWhitespace) {
            next();
        }

        if (type != END_TAG && type != START_TAG) {
            throw new XmlPullParserException("unexpected type", this, null);
        }

        return type;
    }

    public void require(int type, String namespace, String name)
            throws XmlPullParserException, IOException {

        if (type != this.type
                || (namespace != null && !namespace.equals(getNamespace()))
                || (name != null && !name.equals(getName()))) {
            throw new XmlPullParserException(
                    "expected: " + TYPES[type] + " {" + namespace + "}" + name, this, null);
        }
    }

    public String nextText() throws XmlPullParserException, IOException {
        if (type != START_TAG) {
            throw new XmlPullParserException("precondition: START_TAG", this, null);
        }

        next();

        String result;
        if (type == TEXT) {
            result = getText();
            next();
        } else {
            result = "";
        }

        if (type != END_TAG) {
            throw new XmlPullParserException("END_TAG expected", this, null);
        }

        return result;
    }

    public void setFeature(String feature, boolean value) throws XmlPullParserException {
        if (XmlPullParser.FEATURE_PROCESS_NAMESPACES.equals(feature)) {
            processNsp = value;
        } else if (isProp(feature, false, "relaxed")) {
            // "http://xmlpull.org/v1/doc/features.html#relaxed"
            relaxed = value;
        } else {
            throw new XmlPullParserException("unsupported feature: " + feature, this, null);
        }
    }

    public void setProperty(String property, Object value) throws XmlPullParserException {
        if (isProp(property, true, "location")) {
            location = String.valueOf(value);
        } else {
            throw new XmlPullParserException("unsupported property: " + property);
        }
    }
}
