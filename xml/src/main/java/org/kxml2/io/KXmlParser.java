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

    private static final int ELEMENTDECL = 11;
    private static final int ENTITYDECL = 12;
    private static final int ATTLISTDECL = 13;
    private static final int NOTATIONDECL = 14;
    private static final int PARAMETER_ENTITY_REF = 15;
    private static final char[] START_COMMENT = { '<', '!', '-', '-' };
    private static final char[] END_COMMENT = { '-', '-', '>' };
    private static final char[] COMMENT_DOUBLE_DASH = { '-', '-' };
    private static final char[] START_CDATA = { '<', '!', '[', 'C', 'D', 'A', 'T', 'A', '[' };
    private static final char[] END_CDATA = { ']', ']', '>' };
    private static final char[] START_PROCESSING_INSTRUCTION = { '<', '?' };
    private static final char[] END_PROCESSING_INSTRUCTION = { '?', '>' };
    private static final char[] START_DOCTYPE = { '<', '!', 'D', 'O', 'C', 'T', 'Y', 'P', 'E' };
    private static final char[] SYSTEM = { 'S', 'Y', 'S', 'T', 'E', 'M' };
    private static final char[] PUBLIC = { 'P', 'U', 'B', 'L', 'I', 'C' };
    private static final char[] START_ELEMENT = { '<', '!', 'E', 'L', 'E', 'M', 'E', 'N', 'T' };
    private static final char[] START_ATTLIST = { '<', '!', 'A', 'T', 'T', 'L', 'I', 'S', 'T' };
    private static final char[] START_ENTITY = { '<', '!', 'E', 'N', 'T', 'I', 'T', 'Y' };
    private static final char[] START_NOTATION = { '<', '!', 'N', 'O', 'T', 'A', 'T', 'I', 'O', 'N' };
    private static final char[] EMPTY = new char[] { 'E', 'M', 'P', 'T', 'Y' };
    private static final char[] ANY = new char[]{ 'A', 'N', 'Y' };
    private static final char[] NDATA = new char[]{ 'N', 'D', 'A', 'T', 'A' };
    private static final char[] NOTATION = new char[]{ 'N', 'O', 'T', 'A', 'T', 'I', 'O', 'N' };
    private static final char[] REQUIRED = new char[] { 'R', 'E', 'Q', 'U', 'I', 'R', 'E', 'D' };
    private static final char[] IMPLIED = new char[] { 'I', 'M', 'P', 'L', 'I', 'E', 'D' };
    private static final char[] FIXED = new char[] { 'F', 'I', 'X', 'E', 'D' };

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

    public int next() throws XmlPullParserException, IOException {
        return next(false);
    }

    public int nextToken() throws XmlPullParserException, IOException {
        return next(true);
    }

    private int next(boolean justOneToken) throws IOException, XmlPullParserException {
        if (reader == null) {
            throw new XmlPullParserException("setInput() must be called first.", this, null);
        }

        if (type == END_TAG) {
            depth--;
        }

        // degenerated needs to be handled before error because of possible
        // processor expectations(!)

        if (degenerated) {
            degenerated = false;
            type = END_TAG;
            return type;
        }

        if (error != null) {
            if (justOneToken) {
                text = error;
                type = COMMENT;
                error = null;
                return type;
            } else {
                error = null;
            }
        }

        type = peekType(false);

        if (type == XML_DECLARATION) {
            readXmlDeclaration();
            type = peekType(false);
        }

        text = null;
        isWhitespace = true;
        prefix = null;
        name = null;
        namespace = null;
        attributeCount = -1;

        while (true) {
            switch (type) {

            /*
             * Return immediately after encountering a start tag, end tag, or
             * the end of the document.
             */
            case START_TAG:
                parseStartTag(false);
                return type;
            case END_TAG:
                readEndTag();
                return type;
            case END_DOCUMENT:
                return type;

            /*
             * Return after any text token when we're looking for a single
             * token. Otherwise concatenate all text between tags.
             */
            case ENTITY_REF:
                if (justOneToken) {
                    StringBuilder entityTextBuilder = new StringBuilder();
                    readEntity(entityTextBuilder, true, ValueContext.TEXT);
                    text = entityTextBuilder.toString();
                    break;
                }
                // fall-through
            case TEXT:
                text = readValue('<', !justOneToken, ValueContext.TEXT);
                if (depth == 0 && isWhitespace) {
                    type = IGNORABLE_WHITESPACE;
                }
                break;
            case CDSECT:
                read(START_CDATA);
                text = readUntil(END_CDATA, true);
                break;

            /*
             * Comments, processing instructions and declarations are returned
             * when we're looking for a single token. Otherwise they're skipped.
             */
            case COMMENT:
                String commentText = readComment(justOneToken);
                if (justOneToken) {
                    text = commentText;
                }
                break;
            case PROCESSING_INSTRUCTION:
                read(START_PROCESSING_INSTRUCTION);
                String processingInstruction = readUntil(END_PROCESSING_INSTRUCTION, justOneToken);
                if (justOneToken) {
                    text = processingInstruction;
                }
                break;
            case DOCDECL:
                readDoctype();
                if (justOneToken) {
                    text = ""; // TODO: support capturing the doctype text
                }
                break;

            default:
                throw new XmlPullParserException("Unexpected token", this, null);
            }

            if (justOneToken) {
                return type;
            }

            if (type == IGNORABLE_WHITESPACE) {
                text = null;
            }

            /*
             * We've read all that we can of a non-empty text block. Always
             * report this as text, even if it was a CDATA block or entity
             * reference.
             */
            int peek = peekType(false);
            if (text != null && !text.isEmpty() && peek < TEXT) {
                type = TEXT;
                return type;
            }

            type = peek;
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
        int start = position;
        StringBuilder result = null;

        if (returnText && text != null) {
            result = new StringBuilder();
            result.append(text);
        }

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
                    position++;
                    continue search;
                }
            }

            break;
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
    private void readXmlDeclaration() throws IOException, XmlPullParserException {
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
    }

    private String readComment(boolean returnText) throws IOException, XmlPullParserException {
        read(START_COMMENT);

        if (relaxed) {
            return readUntil(END_COMMENT, returnText);
        }

        String commentText = readUntil(COMMENT_DOUBLE_DASH, returnText);
        if (peekCharacter() != '>') {
            throw new XmlPullParserException("Comments may not contain --", this, null);
        }
        position++;
        return commentText;
    }

    /**
     * Read the document's DTD. Although this parser is non-validating, the DTD
     * must be parsed to capture entity values and default attribute values.
     */
    private void readDoctype() throws IOException, XmlPullParserException {
        read(START_DOCTYPE);
        skip();
        readName();
        readExternalId(true);
        skip();
        if (peekCharacter() == '[') {
            readInternalSubset();
        }
        skip();
        read('>');
    }

    /**
     * Reads an external ID of one of these two forms:
     *   SYSTEM "quoted system name"
     *   PUBLIC "quoted public id" "quoted system name"
     *
     * If the system name is not required, this also supports lone public IDs of
     * this form:
     *   PUBLIC "quoted public id"
     *
     * Returns true if any ID was read.
     */
    private boolean readExternalId(boolean requireSystemName)
            throws IOException, XmlPullParserException {
        skip();
        int c = peekCharacter();

        if (c == 'S') {
            read(SYSTEM);
        } else if (c == 'P') {
            read(PUBLIC);
            skip();
            readQuotedId();
        } else {
            return false;
        }

        skip();

        if (!requireSystemName) {
            int delimiter = peekCharacter();
            if (delimiter != '"' && delimiter != '\'') {
                return true; // no system name!
            }
        }

        readQuotedId();
        return true;
    }

    /**
     * Reads a quoted string, performing no entity escaping of the contents.
     */
    private void readQuotedId() throws IOException, XmlPullParserException {
        int quote = peekCharacter();
        if (quote != '"' && quote != '\'') {
            throw new XmlPullParserException("Expected a quoted string", this, null);
        }
        position++;
        while (peekCharacter() != quote) {
            position++;
        }
        position++;
    }

    private void readInternalSubset() throws IOException, XmlPullParserException {
        read('[');

        while (true) {
            skip();
            if (peekCharacter() == ']') {
                position++;
                return;
            }

            int declarationType = peekType(true);
            switch (declarationType) {
            case ELEMENTDECL:
                readElementDeclaration();
                break;

            case ATTLISTDECL:
                readAttributeListDeclaration();
                break;

            case ENTITYDECL:
                readEntityDeclaration();
                break;

            case NOTATIONDECL:
                readNotationDeclaration();
                break;

            case PROCESSING_INSTRUCTION:
                read(START_PROCESSING_INSTRUCTION);
                readUntil(END_PROCESSING_INSTRUCTION, false);
                break;

            case COMMENT:
                readComment(false);
                break;

            case PARAMETER_ENTITY_REF:
                throw new XmlPullParserException(
                        "Parameter entity references are not supported", this, null);

            default:
                throw new XmlPullParserException("Unexpected token", this, null);
            }
        }
    }

    /**
     * Read an element declaration. This contains a name and a content spec.
     *   <!ELEMENT foo EMPTY >
     *   <!ELEMENT foo (bar?,(baz|quux)) >
     *   <!ELEMENT foo (#PCDATA|bar)* >
     */
    private void readElementDeclaration() throws IOException, XmlPullParserException {
        read(START_ELEMENT);
        skip();
        readName();
        readContentSpec();
        skip();
        read('>');
    }

    /**
     * Read an element content spec. This is a regular expression-like pattern
     * of names or other content specs. The following operators are supported:
     *   sequence:    (a,b,c)
     *   choice:      (a|b|c)
     *   optional:    a?
     *   one or more: a+
     *   any number:  a*
     *
     * The special name '#PCDATA' is permitted but only if it is the first
     * element of the first group:
     *   (#PCDATA|a|b)
     *
     * The top-level element must be either a choice, a sequence, or one of the
     * special names EMPTY and ANY.
     */
    private void readContentSpec() throws IOException, XmlPullParserException {
        // this implementation is very lenient; it scans for balanced parens only
        skip();
        int c = peekCharacter();
        if (c == '(') {
            int depth = 0;
            do {
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                position++;
                c = peekCharacter();
            } while (depth > 0);

            if (c == '*' || c == '?' || c == '+') {
                position++;
            }
        } else if (c == EMPTY[0]) {
            read(EMPTY);
        } else if (c == ANY[0]) {
            read(ANY);
        } else {
            throw new XmlPullParserException("Expected element content spec", this, null);
        }
    }

    /**
     * Reads an attribute list declaration such as the following:
     *   <!ATTLIST foo
     *       bar CDATA #IMPLIED
     *       quux (a|b|c) "c"
     *       baz NOTATION (a|b|c) #FIXED "c">
     *
     * Each attribute has a name, type and default.
     *
     * Types are one of the built-in types (CDATA, ID, IDREF, IDREFS, ENTITY,
     * ENTITIES, NMTOKEN, or NMTOKENS), an enumerated type "(list|of|options)"
     * or NOTATION followed by an enumerated type.
     *
     * The default is either #REQUIRED, #IMPLIED, #FIXED, a quoted value, or
     * #FIXED with a quoted value.
     */
    private void readAttributeListDeclaration() throws IOException, XmlPullParserException {
        read(START_ATTLIST);
        skip();
        String elementName = readName();

        while (true) {
            skip();
            int c = peekCharacter();
            if (c == '>') {
                position++;
                return;
            }

            // attribute name
            String attributeName = readName();

            // attribute type
            skip();
            if (position + 1 >= limit && !fillBuffer(2)) {
                throw new XmlPullParserException("Malformed attribute list", this, null);
            }
            if (buffer[position] == NOTATION[0] && buffer[position + 1] == NOTATION[1]) {
                read(NOTATION);
                skip();
            }
            c = peekCharacter();
            if (c == '(') {
                position++;
                while (true) {
                    skip();
                    readName();
                    skip();
                    c = peekCharacter();
                    if (c == ')') {
                        position++;
                        break;
                    } else if (c == '|') {
                        position++;
                    } else {
                        throw new XmlPullParserException("Malformed attribute type", this, null);
                    }
                }
            } else {
                readName();
            }

            // default value
            skip();
            c = peekCharacter();
            if (c == '#') {
                position++;
                c = peekCharacter();
                if (c == 'R') {
                    read(REQUIRED);
                } else if (c == 'I') {
                    read(IMPLIED);
                } else if (c == 'F') {
                    read(FIXED);
                } else {
                    throw new XmlPullParserException("Malformed attribute type", this, null);
                }
                skip();
                c = peekCharacter();
            }
            if (c == '"' || c == '\'') {
                position++;
                // TODO: does this do escaping correctly?
                String value = readValue((char) c, true, ValueContext.ATTRIBUTE);
                position++;
                defineAttributeDefault(elementName, attributeName, value);
            }
        }
    }

    private void defineAttributeDefault(String elementName, String attributeName, String value) {
        // TODO: stash this attribute so we can recall it later
    }

    /**
     * Read an entity declaration. The value of internal entities are inline:
     *   <!ENTITY foo "bar">
     *
     * The values of external entities must be retrieved by URL or path:
     *   <!ENTITY foo SYSTEM "http://host/file">
     *   <!ENTITY foo PUBLIC "-//Android//Foo//EN" "http://host/file">
     *   <!ENTITY foo SYSTEM "../file.png" NDATA png>
     *
     * Entities may be general or parameterized. Parameterized entities are
     * marked by a percent sign. Such entities may only be used in the DTD:
     *   <!ENTITY % foo "bar">
     */
    private void readEntityDeclaration() throws IOException, XmlPullParserException {
        read(START_ENTITY);
        boolean generalEntity = true;

        skip();
        if (peekCharacter() == '%') {
            generalEntity = false;
            position++;
            skip();
        }

        String name = readName();

        skip();
        int quote = peekCharacter();
        if (quote == '"' || quote == '\'') {
            position++;
            String value = readValue((char) quote, true, ValueContext.ENTITY_DECLARATION);
            position++;
            if (generalEntity) {
                defineEntityReplacementText(name, value); // TODO: test parameter and general entity
            }
        } else if (readExternalId(true)) {
            skip();
            if (peekCharacter() == NDATA[0]) {
                read(NDATA);
                skip();
                readName();
            }
        } else {
            throw new XmlPullParserException("Expected entity value or external ID", this, null);
        }

        skip();
        read('>');
    }

    private void readNotationDeclaration() throws IOException, XmlPullParserException {
        read(START_NOTATION);
        skip();
        readName();
        if (!readExternalId(false)) {
            throw new XmlPullParserException(
                    "Expected external ID or public ID for notation", this, null);
        }
        skip();
        read('>');
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

        if (name.equals(elementStack[sp + 3])) {
            namespace = elementStack[sp];
            prefix = elementStack[sp + 1];
            name = elementStack[sp + 2];
        } else if (!relaxed) {
            throw new XmlPullParserException(
                    "expected: /" + elementStack[sp + 3] + " read: " + name, this, null);
        }
    }

    /**
     * Returns the type of the next token.
     */
    private int peekType(boolean inDeclaration) throws IOException, XmlPullParserException {
        if (position >= limit && !fillBuffer(1)) {
            return END_DOCUMENT;
        }

        switch (buffer[position]) {
        case '&':
            return ENTITY_REF; // &
        case '<':
            if (position + 3 >= limit && !fillBuffer(4)) {
                throw new XmlPullParserException("Dangling <", this, null);
            }

            switch (buffer[position + 1]) {
            case '/':
                return END_TAG; // </
            case '?':
                // we're looking for "<?xml " with case insensitivity
                if ((position + 5 < limit || fillBuffer(6))
                        && (buffer[position + 2] == 'x' || buffer[position + 2] == 'X')
                        && (buffer[position + 3] == 'm' || buffer[position + 3] == 'M')
                        && (buffer[position + 4] == 'l' || buffer[position + 4] == 'L')
                        && (buffer[position + 5] == ' ')) {
                    return XML_DECLARATION; // <?xml
                } else {
                    return PROCESSING_INSTRUCTION; // <?
                }
            case '!':
                switch (buffer[position + 2]) {
                case 'D':
                    return DOCDECL; // <!D
                case '[':
                    return CDSECT; // <![
                case '-':
                    return COMMENT; // <!-
                case 'E':
                    switch (buffer[position + 3]) {
                    case 'L':
                        return ELEMENTDECL; // <!EL
                    case 'N':
                        return ENTITYDECL; // <!EN
                    default:
                        throw new XmlPullParserException("Unexpected <!", this, null);
                    }
                case 'A':
                    return ATTLISTDECL;  // <!A
                case 'N':
                    return NOTATIONDECL; // <!N
                }
            default:
                return START_TAG; // <
            }
        case '%':
            return inDeclaration ? PARAMETER_ENTITY_REF : TEXT;
        default:
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

                attributes[i] = readValue(delimiter, true, ValueContext.ATTRIBUTE);

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
    private void readEntity(StringBuilder out, boolean isEntityToken, ValueContext valueContext)
            throws IOException, XmlPullParserException {
        int start = out.length();

        if (buffer[position++] != '&') {
            throw new AssertionError();
        }

        out.append('&');

        while (true) {
            int c = peekCharacter();

            if (c == ';') {
                out.append(';');
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

        String code = out.substring(start + 1, out.length() - 1);

        if (isEntityToken) {
            name = code;
        }

        String resolved;
        if (code.startsWith("#")) {
            try {
                int c = code.startsWith("#x")
                        ? Integer.parseInt(code.substring(2), 16)
                        : Integer.parseInt(code.substring(1));
                out.delete(start, out.length());
                out.appendCodePoint(c);
                unresolved = false;
            } catch (NumberFormatException notANumber) {
                throw new XmlPullParserException("Invalid character reference: &" + code);
            } catch (IllegalArgumentException invalidCodePoint) {
                throw new XmlPullParserException("Invalid character reference: &" + code);
            }
        } else if (valueContext == ValueContext.ENTITY_DECLARATION) {
            // keep the unresolved &code; in the text
        } else if ((resolved = entityMap.get(code)) != null) {
            out.delete(start, out.length());
            out.append(resolved);
            unresolved = false;
        } else {
            // keep the unresolved entity "&code;" in the text for relaxed clients
            unresolved = true;
            if (!isEntityToken) {
                checkRelaxed("unresolved: &" + code + ";");
            }
        }
    }

    /**
     * Where a value is found impacts how that value is interpreted. For
     * example, in attributes, "\n" must be replaced with a space character. In
     * text, "]]>" is forbidden. In entity declarations, named references are
     * not resolved.
     */
    enum ValueContext {
        ATTRIBUTE,
        TEXT,
        ENTITY_DECLARATION
    }

    /**
     * Returns the current text or attribute value. This also has the side
     * effect of setting isWhitespace to false if a non-whitespace character is
     * encountered.
     *
     * @param delimiter {@code <} for text, {@code "} and {@code '} for quoted
     *     attributes, or a space for unquoted attributes.
     */
    private String readValue(char delimiter, boolean resolveEntities,
            ValueContext valueContext) throws IOException, XmlPullParserException {

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
        if (valueContext == ValueContext.TEXT && text != null) {
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
                    && (c != '\n' || valueContext != ValueContext.ATTRIBUTE)
                    && c != '&'
                    && c != '<'
                    && (c != ']' || valueContext != ValueContext.TEXT)) {
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
                c = (valueContext == ValueContext.ATTRIBUTE) ? ' ' : '\n';

            } else if (c == '\n') {
                c = ' ';

            } else if (c == '&') {
                isWhitespace = false; // TODO: what if the entity resolves to whitespace?
                readEntity(result, false, valueContext);
                start = position;
                continue;

            } else if (c == '<') {
                if (valueContext == ValueContext.ATTRIBUTE) {
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
