/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.io;

/**
 * Wraps an existing {@link OutputStream} and writes big-endian typed data to it.
 * Typically, this stream can be read in by DataInputStream. Types that can be
 * written include byte, 16-bit short, 32-bit int, 32-bit float, 64-bit long,
 * 64-bit double, byte strings, and {@link DataInput MUTF-8} encoded strings.
 *
 * @see DataInputStream
 */
public class DataOutputStream extends FilterOutputStream implements DataOutput {
    private final byte[] buff = new byte[8];

    /**
     * The number of bytes written out so far.
     */
    protected int written;

    /**
     * Constructs a new {@code DataOutputStream} on the {@code OutputStream}
     * {@code out}. Note that data written by this stream is not in a human
     * readable form but can be reconstructed by using a {@link DataInputStream}
     * on the resulting output.
     *
     * @param out
     *            the target stream for writing.
     */
    public DataOutputStream(OutputStream out) {
        super(out);
    }

    /**
     * Flushes this stream to ensure all pending data is sent out to the target
     * stream. This implementation then also flushes the target stream.
     *
     * @throws IOException
     *             if an error occurs attempting to flush this stream.
     */
    @Override
    public void flush() throws IOException {
        super.flush();
    }

    /**
     * Returns the total number of bytes written to the target stream so far.
     *
     * @return the number of bytes written to the target stream.
     */
    public final int size() {
        if (written < 0) {
            written = Integer.MAX_VALUE;
        }
        return written;
    }

    /**
     * Writes {@code count} bytes from the byte array {@code buffer} starting at
     * {@code offset} to the target stream.
     *
     * @param buffer
     *            the buffer to write to the target stream.
     * @param offset
     *            the index of the first byte in {@code buffer} to write.
     * @param count
     *            the number of bytes from the {@code buffer} to write.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @throws NullPointerException
     *             if {@code buffer} is {@code null}.
     * @see DataInputStream#readFully(byte[])
     * @see DataInputStream#readFully(byte[], int, int)
     */
    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        if (buffer == null) {
            throw new NullPointerException("buffer == null");
        }
        out.write(buffer, offset, count);
        written += count;
    }

    /**
     * Writes a byte to the target stream. Only the least significant byte of
     * the integer {@code oneByte} is written.
     *
     * @param oneByte
     *            the byte to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readByte()
     */
    @Override
    public void write(int oneByte) throws IOException {
        out.write(oneByte);
        written++;
    }

    /**
     * Writes a boolean to the target stream.
     *
     * @param val
     *            the boolean value to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readBoolean()
     */
    public final void writeBoolean(boolean val) throws IOException {
        out.write(val ? 1 : 0);
        written++;
    }

    /**
     * Writes an 8-bit byte to the target stream. Only the least significant
     * byte of the integer {@code val} is written.
     *
     * @param val
     *            the byte value to write to the target stream.
     * @throws IOException
     *             if an error occurs while writing to the target stream.
     * @see DataInputStream#readByte()
     * @see DataInputStream#readUnsignedByte()
     */
    public final void writeByte(int val) throws IOException {
        out.write(val);
        written++;
    }

    public final void writeBytes(String str) throws IOException {
        if (str.length() == 0) {
            return;
        }
        byte[] bytes = new byte[str.length()];
        for (int index = 0; index < str.length(); index++) {
            bytes[index] = (byte) str.charAt(index);
        }
        out.write(bytes);
        written += bytes.length;
    }

    public final void writeChar(int val) throws IOException {
        buff[0] = (byte) (val >> 8);
        buff[1] = (byte) val;
        out.write(buff, 0, 2);
        written += 2;
    }

    public final void writeChars(String str) throws IOException {
        byte[] newBytes = new byte[str.length() * 2];
        for (int index = 0; index < str.length(); index++) {
            int newIndex = index == 0 ? index : index * 2;
            newBytes[newIndex] = (byte) (str.charAt(index) >> 8);
            newBytes[newIndex + 1] = (byte) str.charAt(index);
        }
        out.write(newBytes);
        written += newBytes.length;
    }

    public final void writeDouble(double val) throws IOException {
        writeLong(Double.doubleToLongBits(val));
    }

    public final void writeFloat(float val) throws IOException {
        writeInt(Float.floatToIntBits(val));
    }

    public final void writeInt(int val) throws IOException {
        buff[0] = (byte) (val >> 24);
        buff[1] = (byte) (val >> 16);
        buff[2] = (byte) (val >> 8);
        buff[3] = (byte) val;
        out.write(buff, 0, 4);
        written += 4;
    }

    public final void writeLong(long val) throws IOException {
        buff[0] = (byte) (val >> 56);
        buff[1] = (byte) (val >> 48);
        buff[2] = (byte) (val >> 40);
        buff[3] = (byte) (val >> 32);
        buff[4] = (byte) (val >> 24);
        buff[5] = (byte) (val >> 16);
        buff[6] = (byte) (val >> 8);
        buff[7] = (byte) val;
        out.write(buff, 0, 8);
        written += 8;
    }

    int writeLongToBuffer(long val, byte[] buffer, int offset) throws IOException {
        buffer[offset++] = (byte) (val >> 56);
        buffer[offset++] = (byte) (val >> 48);
        buffer[offset++] = (byte) (val >> 40);
        buffer[offset++] = (byte) (val >> 32);
        buffer[offset++] = (byte) (val >> 24);
        buffer[offset++] = (byte) (val >> 16);
        buffer[offset++] = (byte) (val >> 8);
        buffer[offset++] = (byte) val;
        return offset;
    }

    public final void writeShort(int val) throws IOException {
        buff[0] = (byte) (val >> 8);
        buff[1] = (byte) val;
        out.write(buff, 0, 2);
        written += 2;
    }

    int writeShortToBuffer(int val, byte[] buffer, int offset) throws IOException {
        buffer[offset++] = (byte) (val >> 8);
        buffer[offset++] = (byte) val;
        return offset;
    }

    public final void writeUTF(String str) throws IOException {
        long utfCount = countUTFBytes(str);
        if (utfCount > 65535) {
            throw new UTFDataFormatException("String more than 65535 UTF bytes long");
        }
        byte[] buffer = new byte[(int)utfCount + 2];
        int offset = 0;
        offset = writeShortToBuffer((int) utfCount, buffer, offset);
        offset = writeUTFBytesToBuffer(str, buffer, offset);
        write(buffer, 0, offset);
    }

    long countUTFBytes(String str) {
        int utfCount = 0, length = str.length();
        for (int i = 0; i < length; i++) {
            int charValue = str.charAt(i);
            if (charValue > 0 && charValue <= 127) {
                utfCount++;
            } else if (charValue <= 2047) {
                utfCount += 2;
            } else {
                utfCount += 3;
            }
        }
        return utfCount;
    }

    int writeUTFBytesToBuffer(String str, byte[] buffer, int offset) throws IOException {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            int charValue = str.charAt(i);
            if (charValue > 0 && charValue <= 127) {
                buffer[offset++] = (byte) charValue;
            } else if (charValue <= 2047) {
                buffer[offset++] = (byte) (0xc0 | (0x1f & (charValue >> 6)));
                buffer[offset++] = (byte) (0x80 | (0x3f & charValue));
            } else {
                buffer[offset++] = (byte) (0xe0 | (0x0f & (charValue >> 12)));
                buffer[offset++] = (byte) (0x80 | (0x3f & (charValue >> 6)));
                buffer[offset++] = (byte) (0x80 | (0x3f & charValue));
             }
        }
        return offset;
    }
}
