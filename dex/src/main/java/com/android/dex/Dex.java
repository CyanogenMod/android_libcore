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

package com.android.dex;

import com.android.dex.util.ByteInput;
import com.android.dex.util.ByteOutput;
import com.android.dex.util.FileUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.AbstractList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.Adler32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The bytes of a dex file in memory for reading and writing. All int offsets
 * are unsigned.
 */
public final class Dex {
    private static final int CHECKSUM_OFFSET = 8;
    private static final int CHECKSUM_SIZE = 4;
    private static final int SIGNATURE_OFFSET = CHECKSUM_OFFSET + CHECKSUM_SIZE;
    private static final int SIGNATURE_SIZE = 20;

    private ByteBuffer data;
    private final TableOfContents tableOfContents = new TableOfContents();
    private int nextSectionStart = 0;

    private final List<String> strings = new AbstractList<String>() {
        @Override public String get(int index) {
            checkBounds(index, tableOfContents.stringIds.size);
            return open(tableOfContents.stringIds.off + (index * SizeOf.STRING_ID_ITEM))
                    .readString();
        }
        @Override public int size() {
            return tableOfContents.stringIds.size;
        }
    };

    private final List<Integer> typeIds = new AbstractList<Integer>() {
        @Override public Integer get(int index) {
            checkBounds(index, tableOfContents.typeIds.size);
            return open(tableOfContents.typeIds.off + (index * SizeOf.TYPE_ID_ITEM)).readInt();
        }
        @Override public int size() {
            return tableOfContents.typeIds.size;
        }
    };

    private final List<String> typeNames = new AbstractList<String>() {
        @Override public String get(int index) {
            checkBounds(index, tableOfContents.typeIds.size);
            return strings.get(typeIds.get(index));
        }
        @Override public int size() {
            return tableOfContents.typeIds.size;
        }
    };

    private final List<ProtoId> protoIds = new AbstractList<ProtoId>() {
        @Override public ProtoId get(int index) {
            checkBounds(index, tableOfContents.protoIds.size);
            return open(tableOfContents.protoIds.off + (SizeOf.PROTO_ID_ITEM * index))
                    .readProtoId();
        }
        @Override public int size() {
            return tableOfContents.protoIds.size;
        }
    };

    private final List<FieldId> fieldIds = new AbstractList<FieldId>() {
        @Override public FieldId get(int index) {
            checkBounds(index, tableOfContents.fieldIds.size);
            return open(tableOfContents.fieldIds.off + (SizeOf.MEMBER_ID_ITEM * index))
                    .readFieldId();
        }
        @Override public int size() {
            return tableOfContents.fieldIds.size;
        }
    };

    private final List<MethodId> methodIds = new AbstractList<MethodId>() {
        @Override public MethodId get(int index) {
            checkBounds(index, tableOfContents.methodIds.size);
            return open(tableOfContents.methodIds.off + (SizeOf.MEMBER_ID_ITEM * index))
                    .readMethodId();
        }
        @Override public int size() {
            return tableOfContents.methodIds.size;
        }
    };

    /**
     * Creates a new dex that reads from {@code data}. It is an error to modify
     * {@code data} after using it to create a dex buffer.
     */
    public Dex(byte[] data) throws IOException {
        this.data = ByteBuffer.wrap(data);
        this.data.order(ByteOrder.LITTLE_ENDIAN);
        this.tableOfContents.readFrom(this);
    }

    /**
     * Creates a new empty dex of the specified size.
     */
    public Dex(int byteCount) throws IOException {
        this.data = ByteBuffer.wrap(new byte[byteCount]);
        this.data.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Creates a new dex buffer of the dex in {@code in}, and closes {@code in}.
     */
    public Dex(InputStream in) throws IOException {
        loadFrom(in);
    }

    /**
     * Creates a new dex buffer from the dex file {@code file}.
     */
    public Dex(File file) throws IOException {
        if (FileUtils.hasArchiveSuffix(file.getName())) {
            ZipFile zipFile = new ZipFile(file);
            ZipEntry entry = zipFile.getEntry(DexFormat.DEX_IN_JAR_NAME);
            if (entry != null) {
                loadFrom(zipFile.getInputStream(entry));
                zipFile.close();
            } else {
                throw new DexException("Expected " + DexFormat.DEX_IN_JAR_NAME + " in " + file);
            }
        } else if (file.getName().endsWith(".dex")) {
            loadFrom(new FileInputStream(file));
        } else {
            throw new DexException("unknown output extension: " + file);
        }
    }

    /**
     * Creates a new dex from the contents of {@code bytes}. This API supports
     * both {@code .dex} and {@code .odex} input. Calling this constructor
     * transfers ownership of {@code bytes} to the returned Dex: it is an error
     * to access the buffer after calling this method.
     */
    public static Dex create(ByteBuffer bytes) throws IOException {
        bytes.order(ByteOrder.LITTLE_ENDIAN);

        // if it's an .odex file, set position and limit to the .dex section
        if (bytes.get(0) == 'd'
                && bytes.get(1) == 'e'
                && bytes.get(2) == 'y'
                && bytes.get(3) == '\n') {
            bytes.position(8);
            int offset = bytes.getInt();
            int length = bytes.getInt();
            bytes.position(offset);
            bytes.limit(offset + length);
        }

        // TODO: don't copy the bytes; use the ByteBuffer directly
        byte[] data = new byte[bytes.remaining()];
        bytes.get(data);
        return new Dex(data);
    }

    private void loadFrom(InputStream in) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];

        int count;
        while ((count = in.read(buffer)) != -1) {
            bytesOut.write(buffer, 0, count);
        }
        in.close();

        this.data = ByteBuffer.wrap(bytesOut.toByteArray());
        this.data.order(ByteOrder.LITTLE_ENDIAN);
        this.tableOfContents.readFrom(this);
    }

    private static void checkBounds(int index, int length) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("index:" + index + ", length=" + length);
        }
    }

    public void writeTo(OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        ByteBuffer data = this.data.duplicate(); // positioned ByteBuffers aren't thread safe
        data.clear();
        while (data.hasRemaining()) {
            int count = Math.min(buffer.length, data.remaining());
            data.get(buffer, 0, count);
            out.write(buffer, 0, count);
        }
    }

    public void writeTo(File dexOut) throws IOException {
        OutputStream out = new FileOutputStream(dexOut);
        writeTo(out);
        out.close();
    }

    public TableOfContents getTableOfContents() {
        return tableOfContents;
    }

    public Section open(int position) {
        if (position < 0 || position >= data.capacity()) {
            throw new IllegalArgumentException("position=" + position
                    + " length=" + data.capacity());
        }
        ByteBuffer sectionData = data.duplicate();
        sectionData.order(ByteOrder.LITTLE_ENDIAN); // necessary?
        sectionData.position(position);
        sectionData.limit(data.capacity());
        return new Section("section", sectionData);
    }

    public Section appendSection(int maxByteCount, String name) {
        if ((maxByteCount & 3) != 0) {
            throw new IllegalStateException("Not four byte aligned!");
        }
        int limit = nextSectionStart + maxByteCount;
        ByteBuffer sectionData = data.duplicate();
        sectionData.order(ByteOrder.LITTLE_ENDIAN); // necessary?
        sectionData.position(nextSectionStart);
        sectionData.limit(limit);
        Section result = new Section(name, sectionData);
        nextSectionStart = limit;
        return result;
    }

    public int getLength() {
        return data.capacity();
    }

    public int getNextSectionStart() {
        return nextSectionStart;
    }

    private static int fourByteAlign(int position) {
        return (position + 3) & ~3;
    }

    /**
     * Returns a copy of the the bytes of this dex.
     */
    public byte[] getBytes() {
        ByteBuffer data = this.data.duplicate(); // positioned ByteBuffers aren't thread safe
        byte[] result = new byte[data.capacity()];
        data.position(0);
        data.get(result);
        return result;
    }

    public List<String> strings() {
        return strings;
    }

    public List<Integer> typeIds() {
        return typeIds;
    }

    public List<String> typeNames() {
        return typeNames;
    }

    public List<ProtoId> protoIds() {
        return protoIds;
    }

    public List<FieldId> fieldIds() {
        return fieldIds;
    }

    public List<MethodId> methodIds() {
        return methodIds;
    }

    public Iterable<ClassDef> classDefs() {
        return new Iterable<ClassDef>() {
            public Iterator<ClassDef> iterator() {
                if (!tableOfContents.classDefs.exists()) {
                    return Collections.<ClassDef>emptySet().iterator();
                }
                return new Iterator<ClassDef>() {
                    private Dex.Section in = open(tableOfContents.classDefs.off);
                    private int count = 0;

                    public boolean hasNext() {
                        return count < tableOfContents.classDefs.size;
                    }
                    public ClassDef next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        count++;
                        return in.readClassDef();
                    }
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    public TypeList readTypeList(int offset) {
        if (offset == 0) {
            return TypeList.EMPTY;
        }
        return open(offset).readTypeList();
    }

    public ClassData readClassData(ClassDef classDef) {
        int offset = classDef.getClassDataOffset();
        if (offset == 0) {
            throw new IllegalArgumentException("offset == 0");
        }
        return open(offset).readClassData();
    }

    public Code readCode(ClassData.Method method) {
        int offset = method.getCodeOffset();
        if (offset == 0) {
            throw new IllegalArgumentException("offset == 0");
        }
        return open(offset).readCode();
    }

    /**
     * Returns the signature of all but the first 32 bytes of this dex. The
     * first 32 bytes of dex files are not specified to be included in the
     * signature.
     */
    public byte[] computeSignature() throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError();
        }
        byte[] buffer = new byte[8192];
        ByteBuffer data = this.data.duplicate(); // positioned ByteBuffers aren't thread safe
        data.limit(data.capacity());
        data.position(SIGNATURE_OFFSET + SIGNATURE_SIZE);
        while (data.hasRemaining()) {
            int count = Math.min(buffer.length, data.remaining());
            data.get(buffer, 0, count);
            digest.update(buffer, 0, count);
        }
        return digest.digest();
    }

    /**
     * Returns the checksum of all but the first 12 bytes of {@code dex}.
     */
    public int computeChecksum() throws IOException {
        Adler32 adler32 = new Adler32();
        byte[] buffer = new byte[8192];
        ByteBuffer data = this.data.duplicate(); // positioned ByteBuffers aren't thread safe
        data.limit(data.capacity());
        data.position(CHECKSUM_OFFSET + CHECKSUM_SIZE);
        while (data.hasRemaining()) {
            int count = Math.min(buffer.length, data.remaining());
            data.get(buffer, 0, count);
            adler32.update(buffer, 0, count);
        }
        return (int) adler32.getValue();
    }

    /**
     * Generates the signature and checksum of the dex file {@code out} and
     * writes them to the file.
     */
    public void writeHashes() throws IOException {
        open(SIGNATURE_OFFSET).write(computeSignature());
        open(CHECKSUM_OFFSET).writeInt(computeChecksum());
    }

    public final class Section implements ByteInput, ByteOutput {
        private final String name;
        private final ByteBuffer data;

        private Section(String name, ByteBuffer data) {
            this.name = name;
            this.data = data;
        }

        public int getPosition() {
            return data.position();
        }

        public int readInt() {
            return data.getInt();
        }

        public short readShort() {
            return data.getShort();
        }

        public int readUnsignedShort() {
            return readShort() & 0xffff;
        }

        public byte readByte() {
            return data.get();
        }

        public byte[] readByteArray(int length) {
            byte[] result = new byte[length];
            data.get(result);
            return result;
        }

        public short[] readShortArray(int length) {
            short[] result = new short[length];
            for (int i = 0; i < length; i++) {
                result[i] = readShort();
            }
            return result;
        }

        public int readUleb128() {
            return Leb128.readUnsignedLeb128(this);
        }

        public int readSleb128() {
            return Leb128.readSignedLeb128(this);
        }

        public TypeList readTypeList() {
            int size = readInt();
            short[] types = new short[size];
            for (int i = 0; i < size; i++) {
                types[i] = readShort();
            }
            alignToFourBytes();
            return new TypeList(Dex.this, types);
        }

        public String readString() {
            int offset = readInt();
            int savedPosition = data.position();
            int savedLimit = data.limit();
            data.position(offset);
            data.limit(data.capacity());
            try {
                int expectedLength = readUleb128();
                String result = Mutf8.decode(this, new char[expectedLength]);
                if (result.length() != expectedLength) {
                    throw new DexException("Declared length " + expectedLength
                            + " doesn't match decoded length of " + result.length());
                }
                return result;
            } catch (UTFDataFormatException e) {
                throw new DexException(e);
            } finally {
                data.position(savedPosition);
                data.limit(savedLimit);
            }
        }

        public FieldId readFieldId() {
            int declaringClassIndex = readUnsignedShort();
            int typeIndex = readUnsignedShort();
            int nameIndex = readInt();
            return new FieldId(Dex.this, declaringClassIndex, typeIndex, nameIndex);
        }

        public MethodId readMethodId() {
            int declaringClassIndex = readUnsignedShort();
            int protoIndex = readUnsignedShort();
            int nameIndex = readInt();
            return new MethodId(Dex.this, declaringClassIndex, protoIndex, nameIndex);
        }

        public ProtoId readProtoId() {
            int shortyIndex = readInt();
            int returnTypeIndex = readInt();
            int parametersOffset = readInt();
            return new ProtoId(Dex.this, shortyIndex, returnTypeIndex, parametersOffset);
        }

        public ClassDef readClassDef() {
            int offset = getPosition();
            int type = readInt();
            int accessFlags = readInt();
            int supertype = readInt();
            int interfacesOffset = readInt();
            int sourceFileIndex = readInt();
            int annotationsOffset = readInt();
            int classDataOffset = readInt();
            int staticValuesOffset = readInt();
            return new ClassDef(Dex.this, offset, type, accessFlags, supertype,
                    interfacesOffset, sourceFileIndex, annotationsOffset, classDataOffset,
                    staticValuesOffset);
        }

        private Code readCode() {
            int registersSize = readUnsignedShort();
            int insSize = readUnsignedShort();
            int outsSize = readUnsignedShort();
            int triesSize = readUnsignedShort();
            int debugInfoOffset = readInt();
            int instructionsSize = readInt();
            short[] instructions = readShortArray(instructionsSize);
            Code.Try[] tries = new Code.Try[triesSize];
            Code.CatchHandler[] catchHandlers = new Code.CatchHandler[0];
            if (triesSize > 0) {
                if (instructions.length % 2 == 1) {
                    readShort(); // padding
                }

                for (int i = 0; i < triesSize; i++) {
                    int startAddress = readInt();
                    int instructionCount = readUnsignedShort();
                    int handlerOffset = readUnsignedShort();
                    tries[i] = new Code.Try(startAddress, instructionCount, handlerOffset);
                }

                int catchHandlersSize = readUleb128();
                catchHandlers = new Code.CatchHandler[catchHandlersSize];
                for (int i = 0; i < catchHandlersSize; i++) {
                    catchHandlers[i] = readCatchHandler();
                }
            }
            return new Code(registersSize, insSize, outsSize, debugInfoOffset, instructions,
                    tries, catchHandlers);
        }

        private Code.CatchHandler readCatchHandler() {
            int size = readSleb128();
            int handlersCount = Math.abs(size);
            int[] typeIndexes = new int[handlersCount];
            int[] addresses = new int[handlersCount];
            for (int i = 0; i < handlersCount; i++) {
                typeIndexes[i] = readUleb128();
                addresses[i] = readUleb128();
            }
            int catchAllAddress = size <= 0 ? readUleb128() : -1;
            return new Code.CatchHandler(typeIndexes, addresses, catchAllAddress);
        }

        private ClassData readClassData() {
            int staticFieldsSize = readUleb128();
            int instanceFieldsSize = readUleb128();
            int directMethodsSize = readUleb128();
            int virtualMethodsSize = readUleb128();
            ClassData.Field[] staticFields = readFields(staticFieldsSize);
            ClassData.Field[] instanceFields = readFields(instanceFieldsSize);
            ClassData.Method[] directMethods = readMethods(directMethodsSize);
            ClassData.Method[] virtualMethods = readMethods(virtualMethodsSize);
            return new ClassData(staticFields, instanceFields, directMethods, virtualMethods);
        }

        private ClassData.Field[] readFields(int count) {
            ClassData.Field[] result = new ClassData.Field[count];
            int fieldIndex = 0;
            for (int i = 0; i < count; i++) {
                fieldIndex += readUleb128(); // field index diff
                int accessFlags = readUleb128();
                result[i] = new ClassData.Field(fieldIndex, accessFlags);
            }
            return result;
        }

        private ClassData.Method[] readMethods(int count) {
            ClassData.Method[] result = new ClassData.Method[count];
            int methodIndex = 0;
            for (int i = 0; i < count; i++) {
                methodIndex += readUleb128(); // method index diff
                int accessFlags = readUleb128();
                int codeOff = readUleb128();
                result[i] = new ClassData.Method(methodIndex, accessFlags, codeOff);
            }
            return result;
        }

        /**
         * Returns a byte array containing the bytes from {@code start} to this
         * section's current position.
         */
        private byte[] getBytesFrom(int start) {
            int end = data.position();
            byte[] result = new byte[end - start];
            data.position(start);
            data.get(result);
            return result;
        }

        public Annotation readAnnotation() {
            byte visibility = readByte();
            int start = data.position();
            new EncodedValueReader(this, EncodedValueReader.ENCODED_ANNOTATION).skipValue();
            return new Annotation(Dex.this, visibility, new EncodedValue(getBytesFrom(start)));
        }

        public EncodedValue readEncodedArray() {
            int start = data.position();
            new EncodedValueReader(this, EncodedValueReader.ENCODED_ARRAY).skipValue();
            return new EncodedValue(getBytesFrom(start));
        }

        /**
         * Writes 0x00 until the position is aligned to a multiple of 4.
         */
        public void alignToFourBytes() {
            while ((data.position() & 3) != 0) {
                data.put((byte) 0);
            }
        }

        public void assertFourByteAligned() {
            if ((data.position() & 3) != 0) {
                throw new IllegalStateException("Not four byte aligned!");
            }
        }

        public void write(byte[] bytes) {
            this.data.put(bytes);
        }

        public void writeByte(int b) {
            data.put((byte) b);
        }

        public void writeShort(short i) {
            data.putShort(i);
        }

        public void writeUnsignedShort(int i) {
            short s = (short) i;
            if (i != (s & 0xffff)) {
                throw new IllegalArgumentException("Expected an unsigned short: " + i);
            }
            writeShort(s);
        }

        public void write(short[] shorts) {
            for (short s : shorts) {
                writeShort(s);
            }
        }

        public void writeInt(int i) {
            data.putInt(i);
        }

        public void writeUleb128(int i) {
            try {
                Leb128.writeUnsignedLeb128(this, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new DexException("Section limit " + data.limit() + " exceeded by " + name);
            }
        }

        public void writeSleb128(int i) {
            try {
                Leb128.writeSignedLeb128(this, i);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new DexException("Section limit " + data.limit() + " exceeded by " + name);
            }
        }

        public void writeStringData(String value) {
            try {
                int length = value.length();
                writeUleb128(length);
                write(Mutf8.encode(value));
                writeByte(0);
            } catch (UTFDataFormatException e) {
                throw new AssertionError();
            }
        }

        public void writeTypeList(TypeList typeList) {
            short[] types = typeList.getTypes();
            writeInt(types.length);
            for (short type : types) {
                writeShort(type);
            }
            alignToFourBytes();
        }

        /**
         * Returns the number of bytes remaining in this section.
         */
        public int remaining() {
            return data.remaining();
        }
    }
}
