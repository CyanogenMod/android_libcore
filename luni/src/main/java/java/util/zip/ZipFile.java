/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.util.zip;

import dalvik.system.CloseGuard;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import libcore.io.BufferIterator;
import libcore.io.HeapBufferIterator;
import libcore.io.Streams;

/**
 * This class provides random read access to a <i>ZIP-archive</i> file. You pay more to read
 * the zip file's central directory up front (from the constructor), but if you're using
 * {@link #getEntry} to look up multiple files by name, you get the benefit of this index.
 *
 * <p>If you only want to iterate through all the files (using {@link #entries}, you should
 * consider {@link ZipInputStream}, which provides stream-like read access to a zip file and
 * has a lower up-front cost, and doesn't require an in-memory index. The savings could be
 * particularly large if your zip file has many entries and you only require a few of them.
 *
 * <p>If you want to create a zip file, use {@link ZipOutputStream}. There is no API for updating
 * an existing zip file.
 */
public class ZipFile implements ZipConstants {
    /**
     * General Purpose Bit Flags, Bit 3.
     * If this bit is set, the fields crc-32, compressed
     * size and uncompressed size are set to zero in the
     * local header.  The correct values are put in the
     * data descriptor immediately following the compressed
     * data.  (Note: PKZIP version 2.04g for DOS only
     * recognizes this bit for method 8 compression, newer
     * versions of PKZIP recognize this bit for any
     * compression method.)
     */
    static final int GPBF_DATA_DESCRIPTOR_FLAG = 1 << 3;

    /**
     * General Purpose Bit Flags, Bit 11.
     * Language encoding flag (EFS).  If this bit is set,
     * the filename and comment fields for this file
     * must be encoded using UTF-8.
     */
    static final int GPBF_UTF8_FLAG = 1 << 11;

    /**
     * Open ZIP file for reading.
     */
    public static final int OPEN_READ = 1;

    /**
     * Delete ZIP file when closed.
     */
    public static final int OPEN_DELETE = 4;

    private final String mFilename;

    private File mFileToDeleteOnClose;

    private RandomAccessFile mRaf;

    private final LinkedHashMap<String, ZipEntry> mEntries = new LinkedHashMap<String, ZipEntry>();

    private final CloseGuard guard = CloseGuard.get();

    /**
     * Constructs a new {@code ZipFile} allowing read access to the contents of the given file.
     * @throws ZipException if a ZIP error occurs.
     * @throws IOException if an {@code IOException} occurs.
     */
    public ZipFile(File file) throws ZipException, IOException {
        this(file, OPEN_READ);
    }

    /**
     * Constructs a new {@code ZipFile} allowing read access to the contents of the given file.
     * @throws IOException if an IOException occurs.
     */
    public ZipFile(String name) throws IOException {
        this(new File(name), OPEN_READ);
    }

    /**
     * Constructs a new {@code ZipFile} allowing access to the given file.
     * The {@code mode} must be either {@code OPEN_READ} or {@code OPEN_READ|OPEN_DELETE}.
     *
     * <p>If the {@code OPEN_DELETE} flag is supplied, the file will be deleted at or before the
     * time that the {@code ZipFile} is closed (the contents will remain accessible until
     * this {@code ZipFile} is closed); it also calls {@code File.deleteOnExit}.
     *
     * @throws IOException if an {@code IOException} occurs.
     */
    public ZipFile(File file, int mode) throws IOException {
        mFilename = file.getPath();
        if (mode != OPEN_READ && mode != (OPEN_READ | OPEN_DELETE)) {
            throw new IllegalArgumentException("Bad mode: " + mode);
        }

        if ((mode & OPEN_DELETE) != 0) {
            mFileToDeleteOnClose = file;
            mFileToDeleteOnClose.deleteOnExit();
        } else {
            mFileToDeleteOnClose = null;
        }

        mRaf = new RandomAccessFile(mFilename, "r");

        readCentralDir();
        guard.open("close");
    }

    @Override protected void finalize() throws IOException {
        try {
            if (guard != null) {
                guard.warnIfOpen();
            }
        } finally {
            try {
                super.finalize();
            } catch (Throwable t) {
                throw new AssertionError(t);
            }
        }
    }

    /**
     * Closes this ZIP file. This method is idempotent.
     *
     * @throws IOException
     *             if an IOException occurs.
     */
    public void close() throws IOException {
        guard.close();
        RandomAccessFile raf = mRaf;

        if (raf != null) { // Only close initialized instances
            synchronized(raf) {
                mRaf = null;
                raf.close();
            }
            if (mFileToDeleteOnClose != null) {
                mFileToDeleteOnClose.delete();
                mFileToDeleteOnClose = null;
            }
        }
    }

    private void checkNotClosed() {
        if (mRaf == null) {
            throw new IllegalStateException("Zip file closed");
        }
    }

    /**
     * Returns an enumeration of the entries. The entries are listed in the
     * order in which they appear in the ZIP archive.
     *
     * @return the enumeration of the entries.
     * @throws IllegalStateException if this ZIP file has been closed.
     */
    public Enumeration<? extends ZipEntry> entries() {
        checkNotClosed();
        final Iterator<ZipEntry> iterator = mEntries.values().iterator();

        return new Enumeration<ZipEntry>() {
            public boolean hasMoreElements() {
                checkNotClosed();
                return iterator.hasNext();
            }

            public ZipEntry nextElement() {
                checkNotClosed();
                return iterator.next();
            }
        };
    }

    /**
     * Gets the ZIP entry with the specified name from this {@code ZipFile}.
     *
     * @param entryName
     *            the name of the entry in the ZIP file.
     * @return a {@code ZipEntry} or {@code null} if the entry name does not
     *         exist in the ZIP file.
     * @throws IllegalStateException if this ZIP file has been closed.
     */
    public ZipEntry getEntry(String entryName) {
        checkNotClosed();
        if (entryName == null) {
            throw new NullPointerException("entryName == null");
        }

        ZipEntry ze = mEntries.get(entryName);
        if (ze == null) {
            ze = mEntries.get(entryName + "/");
        }
        return ze;
    }

    /**
     * Returns an input stream on the data of the specified {@code ZipEntry}.
     *
     * @param entry
     *            the ZipEntry.
     * @return an input stream of the data contained in the {@code ZipEntry}.
     * @throws IOException
     *             if an {@code IOException} occurs.
     * @throws IllegalStateException if this ZIP file has been closed.
     */
    public InputStream getInputStream(ZipEntry entry) throws IOException {
        // Make sure this ZipEntry is in this Zip file.  We run it through the name lookup.
        entry = getEntry(entry.getName());
        if (entry == null) {
            return null;
        }

        // Create an InputStream at the right part of the file.
        RandomAccessFile raf = mRaf;
        synchronized (raf) {
            // We don't know the entry data's start position. All we have is the
            // position of the entry's local header. At position 28 we find the
            // length of the extra data. In some cases this length differs from
            // the one coming in the central header.
            RAFStream rafStream = new RAFStream(raf, entry.mLocalHeaderRelOffset + 28);
            DataInputStream is = new DataInputStream(rafStream);
            int localExtraLenOrWhatever = Short.reverseBytes(is.readShort());
            is.close();

            // Skip the name and this "extra" data or whatever it is:
            rafStream.skip(entry.nameLength + localExtraLenOrWhatever);
            rafStream.mLength = rafStream.mOffset + entry.compressedSize;
            if (entry.compressionMethod == ZipEntry.DEFLATED) {
                int bufSize = Math.max(1024, (int)Math.min(entry.getSize(), 65535L));
                return new ZipInflaterInputStream(rafStream, new Inflater(true), bufSize, entry);
            } else {
                return rafStream;
            }
        }
    }

    /**
     * Gets the file name of this {@code ZipFile}.
     *
     * @return the file name of this {@code ZipFile}.
     */
    public String getName() {
        return mFilename;
    }

    /**
     * Returns the number of {@code ZipEntries} in this {@code ZipFile}.
     *
     * @return the number of entries in this file.
     * @throws IllegalStateException if this ZIP file has been closed.
     */
    public int size() {
        checkNotClosed();
        return mEntries.size();
    }

    /**
     * Find the central directory and read the contents.
     *
     * <p>The central directory can be followed by a variable-length comment
     * field, so we have to scan through it backwards.  The comment is at
     * most 64K, plus we have 18 bytes for the end-of-central-dir stuff
     * itself, plus apparently sometimes people throw random junk on the end
     * just for the fun of it.
     *
     * <p>This is all a little wobbly.  If the wrong value ends up in the EOCD
     * area, we're hosed. This appears to be the way that everybody handles
     * it though, so we're in good company if this fails.
     */
    private void readCentralDir() throws IOException {
        /*
         * Scan back, looking for the End Of Central Directory field.  If
         * the archive doesn't have a comment, we'll hit it on the first
         * try.
         *
         * No need to synchronize mRaf here -- we only do this when we
         * first open the Zip file.
         */
        long scanOffset = mRaf.length() - ENDHDR;
        if (scanOffset < 0) {
            throw new ZipException("File too short to be a zip file: " + mRaf.length());
        }

        long stopOffset = scanOffset - 65536;
        if (stopOffset < 0) {
            stopOffset = 0;
        }

        final int ENDHEADERMAGIC = 0x06054b50;
        while (true) {
            mRaf.seek(scanOffset);
            if (Integer.reverseBytes(mRaf.readInt()) == ENDHEADERMAGIC) {
                break;
            }

            scanOffset--;
            if (scanOffset < stopOffset) {
                throw new ZipException("EOCD not found; not a Zip archive?");
            }
        }

        // Read the End Of Central Directory. We could use ENDHDR instead of the magic number 18,
        // but we don't actually need all the header.
        byte[] eocd = new byte[18];
        mRaf.readFully(eocd);

        // Pull out the information we need.
        BufferIterator it = HeapBufferIterator.iterator(eocd, 0, eocd.length, ByteOrder.LITTLE_ENDIAN);
        int diskNumber = it.readShort() & 0xffff;
        int diskWithCentralDir = it.readShort() & 0xffff;
        int numEntries = it.readShort() & 0xffff;
        int totalNumEntries = it.readShort() & 0xffff;
        it.skip(4); // Ignore centralDirSize.
        long centralDirOffset = ((long) it.readInt()) & 0xffffffffL;

        if (numEntries != totalNumEntries || diskNumber != 0 || diskWithCentralDir != 0) {
            throw new ZipException("spanned archives not supported");
        }

        // Seek to the first CDE and read all entries.
        // We have to do this now (from the constructor) rather than lazily because the
        // public API doesn't allow us to throw IOException except from the constructor
        // or from getInputStream.
        RAFStream rafStream = new RAFStream(mRaf, centralDirOffset);
        BufferedInputStream bufferedStream = new BufferedInputStream(rafStream, 4096);
        byte[] hdrBuf = new byte[CENHDR]; // Reuse the same buffer for each entry.
        for (int i = 0; i < numEntries; ++i) {
            ZipEntry newEntry = new ZipEntry(hdrBuf, bufferedStream);
            mEntries.put(newEntry.getName(), newEntry);
        }
    }

    /**
     * Wrap a stream around a RandomAccessFile.  The RandomAccessFile is shared
     * among all streams returned by getInputStream(), so we have to synchronize
     * access to it.  (We can optimize this by adding buffering here to reduce
     * collisions.)
     *
     * <p>We could support mark/reset, but we don't currently need them.
     */
    static class RAFStream extends InputStream {
        private final RandomAccessFile mSharedRaf;
        private long mLength;
        private long mOffset;

        public RAFStream(RandomAccessFile raf, long offset) throws IOException {
            mSharedRaf = raf;
            mOffset = offset;
            mLength = raf.length();
        }

        @Override public int available() throws IOException {
            return (mOffset < mLength ? 1 : 0);
        }

        @Override public int read() throws IOException {
            return Streams.readSingleByte(this);
        }

        @Override public int read(byte[] b, int off, int len) throws IOException {
            synchronized (mSharedRaf) {
                mSharedRaf.seek(mOffset);
                if (len > mLength - mOffset) {
                    len = (int) (mLength - mOffset);
                }
                int count = mSharedRaf.read(b, off, len);
                if (count > 0) {
                    mOffset += count;
                    return count;
                } else {
                    return -1;
                }
            }
        }

        @Override public long skip(long byteCount) throws IOException {
            if (byteCount > mLength - mOffset) {
                byteCount = mLength - mOffset;
            }
            mOffset += byteCount;
            return byteCount;
        }

        public int fill(Inflater inflater, int nativeEndBufSize) throws IOException {
            synchronized (mSharedRaf) {
                int len = Math.min((int) (mLength - mOffset), nativeEndBufSize);
                int cnt = inflater.setFileInput(mSharedRaf.getFD(), mOffset, nativeEndBufSize);
                skip(cnt);
                return len;
            }
        }
    }

    static class ZipInflaterInputStream extends InflaterInputStream {
        private final ZipEntry entry;
        private long bytesRead = 0;

        public ZipInflaterInputStream(InputStream is, Inflater inf, int bsize, ZipEntry entry) {
            super(is, inf, bsize);
            this.entry = entry;
        }

        @Override public int read(byte[] buffer, int off, int nbytes) throws IOException {
            int i = super.read(buffer, off, nbytes);
            if (i != -1) {
                bytesRead += i;
            }
            return i;
        }

        @Override public int available() throws IOException {
            if (closed) {
                // Our superclass will throw an exception, but there's a jtreg test that
                // explicitly checks that the InputStream returned from ZipFile.getInputStream
                // returns 0 even when closed.
                return 0;
            }
            return super.available() == 0 ? 0 : (int) (entry.getSize() - bytesRead);
        }
    }
}
