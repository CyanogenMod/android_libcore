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

package dalvik.system;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import libcore.util.Objects;
import libcore.io.IoUtils;

/**
 * A sampling profiler. It currently is implemented without any
 * virtual machine support, relying solely on {@code
 * Thread.getStackTrace} to collect samples. As such, the overhead is
 * higher than a native approach and it does not provide insight into
 * where time is spent within native code, but it can still provide
 * useful insight into where a program is spending time.
 *
 * <h3>Usage Example</h3>
 *
 * The following example shows how to use the {@code
 * SamplingProfiler}. It samples the current thread's stack to a depth
 * of 12 stack frame elements over two different measurement periods
 * with samples taken every 100 milliseconds. In then prints the
 * results in hprof format to the standard output.
 *
 * <pre> {@code
 * ThreadSet threadSet = SamplingProfiler.newArrayThreadSet(Thread.currentThread());
 * SamplingProfiler profiler = new SamplingProfiler(12, threadSet);
 * profiler.start(100);
 * // period of measurement
 * profiler.stop();
 * // period of non-measurement
 * profiler.start(100);
 * // another period of measurement
 * profiler.stop();
 * profiler.shutdown();
 * HprofWriter writer = new AsciiHprofWriter(profiler.getHprofData(), System.out);
 * writer.write();
 * }</pre>
 *
 * @hide
 */
public final class SamplingProfiler {

    /**
     * Represents sampling profiler data. Can be converted to ASCII or
     * binary hprof-style output using {@link AsciiHprofWriter} or
     * {@link BinaryHprofWriter}.
     * <p>
     * The data includes:
     * <ul>
     * <li>the start time of the last sampling period
     * <li>the history of thread start and end events
     * <li>stack traces with frequency counts
     * <ul>
     */
    public static final class HprofData {

        public static enum ThreadEventType { START, END };

        /**
         * ThreadEvent represents thread creation and death events for
         * reporting. It provides a record of the thread and thread group
         * names for tying samples back to their source thread.
         */
        public static final class ThreadEvent {

            public final ThreadEventType type;
            public final int objectId;
            public final int threadId;
            public final String threadName;
            public final String groupName;
            public final String parentGroupName;

            public static ThreadEvent start(int objectId, int threadId, String threadName,
                                            String groupName, String parentGroupName) {
                return new ThreadEvent(ThreadEventType.START, objectId, threadId,
                                       threadName, groupName, parentGroupName);
            }

            public static ThreadEvent end(int threadId) {
                return new ThreadEvent(ThreadEventType.END, threadId);
            }

            private ThreadEvent(ThreadEventType type, int objectId, int threadId,
                                String threadName, String groupName, String parentGroupName) {
                if (threadName == null) {
                    throw new NullPointerException("threadName == null");
                }
                this.type = ThreadEventType.START;
                this.objectId = objectId;
                this.threadId = threadId;
                this.threadName = threadName;
                this.groupName = groupName;
                this.parentGroupName = parentGroupName;
            }

            private ThreadEvent(ThreadEventType type, int threadId) {
                this.type = ThreadEventType.END;
                this.objectId = -1;
                this.threadId = threadId;
                this.threadName = null;
                this.groupName = null;
                this.parentGroupName = null;
            }

            @Override public int hashCode() {
                int result = 17;
                result = 31 * result + objectId;
                result = 31 * result + threadId;
                result = 31 * result + Objects.hashCode(threadName);
                result = 31 * result + Objects.hashCode(groupName);
                result = 31 * result + Objects.hashCode(parentGroupName);
                return result;
            }

            @Override public boolean equals(Object o) {
                if (!(o instanceof ThreadEvent)) {
                    return false;
                }
                ThreadEvent event = (ThreadEvent) o;
                return (this.type == event.type
                        && this.objectId == event.objectId
                        && this.threadId == event.threadId
                        && Objects.equal(this.threadName, event.threadName)
                        && Objects.equal(this.groupName, event.groupName)
                        && Objects.equal(this.parentGroupName, event.parentGroupName));
            }

            @Override public String toString() {
                switch (type) {
                    case START:
                        return String.format(
                                "THREAD START (obj=%d, id = %d, name=\"%s\", group=\"%s\")",
                                objectId, threadId, threadName, groupName);
                    case END:
                        return String.format("THREAD END (id = %d)", threadId);
                }
                throw new IllegalStateException(type.toString());
            }
        }

        /**
         * A unique stack trace for a specific thread.
         */
        public static final class StackTrace {

            public final int stackTraceId;
            private int threadId;
            private StackTraceElement[] stackFrames;

            private StackTrace() {
                this.stackTraceId = -1;
            }

            public StackTrace(int stackTraceId, int threadId, StackTraceElement[] stackFrames) {
                if (stackFrames == null) {
                    throw new NullPointerException("stackFrames == null");
                }
                this.stackTraceId = stackTraceId;
                this.threadId = threadId;
                this.stackFrames = stackFrames;
            }

            public int getThreadId() {
                return threadId;
            }

            public StackTraceElement[] getStackFrames() {
                return stackFrames;
            }

            @Override public int hashCode() {
                int result = 17;
                result = 31 * result + threadId;
                result = 31 * result + Arrays.hashCode(stackFrames);
                return result;
            }

            @Override public boolean equals(Object o) {
                if (!(o instanceof StackTrace)) {
                    return false;
                }
                StackTrace s = (StackTrace) o;
                return threadId == s.threadId && Arrays.equals(stackFrames, s.stackFrames);
            }

            @Override public String toString() {
                StringBuilder frames = new StringBuilder();
                if (stackFrames.length > 0) {
                    frames.append('\n');
                    for (StackTraceElement stackFrame : stackFrames) {
                        frames.append("\t at ");
                        frames.append(stackFrame);
                        frames.append('\n');
                    }
                } else {
                    frames.append("<empty>");
                }
                return "StackTrace[stackTraceId=" + stackTraceId
                        + ", threadId=" + threadId
                        + ", frames=" + frames + "]";

            }
        }

        /**
         * A read only container combining a stack trace with its frequency.
         */
        public static final class Sample {

            public final StackTrace stackTrace;
            public final int count;

            private Sample(StackTrace stackTrace, int count) {
                if (stackTrace == null) {
                    throw new NullPointerException("stackTrace == null");
                }
                if (count < 0) {
                    throw new IllegalArgumentException("count < 0:" + count);
                }
                this.stackTrace = stackTrace;
                this.count = count;
            }

            @Override public int hashCode() {
                int result = 17;
                result = 31 * result + stackTrace.hashCode();
                result = 31 * result + count;
                return result;
            }

            @Override public boolean equals(Object o) {
                if (!(o instanceof Sample)) {
                    return false;
                }
                Sample s = (Sample) o;
                return count == s.count && stackTrace.equals(s.stackTrace);
            }

            @Override public String toString() {
                return "Sample[count=" + count + " " + stackTrace + "]";
            }

        }

        /**
         * Start of last sampling period.
         */
        private long startMillis;

        /**
         * CONTROL_SETTINGS flags
         */
        private int flags;

        /**
         * stack sampling depth
         */
        private int depth;

        /**
         * List of thread creation and death events.
         */
        private final List<ThreadEvent> threadHistory = new ArrayList<ThreadEvent>();

        /**
         * Map of thread id to a start ThreadEvent
         */
        private final Map<Integer, ThreadEvent> threadIdToThreadEvent
                = new HashMap<Integer, ThreadEvent>();

        /**
         * Map of stack traces to a mutable sample count. The map is
         * provided by the creator of the HprofData so only have
         * mutable access to the int[] cells that contain the sample
         * count. Only an unmodifiable iterator view is available to
         * users of the HprofData.
         */
        private final Map<HprofData.StackTrace, int[]> stackTraces;

        public HprofData(Map<StackTrace, int[]> stackTraces) {
            if (stackTraces == null) {
                throw new NullPointerException("stackTraces == null");
            }
            this.stackTraces = stackTraces;
        }

        /**
         * The start time in milliseconds of the last profiling period.
         */
        public long getStartMillis() {
            return startMillis;
        }

        /**
         * Set the time for the start of the current sampling period.
         */
        public void setStartMillis(long startMillis) {
            this.startMillis = startMillis;
        }

        /**
         * Get the {@link ControlSettings} flags
         */
        public int getFlags() {
            return flags;
        }

        /**
         * Set the {@link ControlSettings} flags
         */
        public void setFlags(int flags) {
            this.flags = flags;
        }

        /**
         * Get the stack sampling depth
         */
        public int getDepth() {
            return depth;
        }

        /**
         * Set the stack sampling depth
         */
        public void setDepth(int depth) {
            this.depth = depth;
        }

        /**
         * Return an unmodifiable history of start and end thread events.
         */
        public List<ThreadEvent> getThreadHistory() {
            return Collections.unmodifiableList(threadHistory);
        }

        /**
         * Return a new set containing the current sample data.
         */
        public Set<Sample> getSamples() {
            Set<Sample> samples = new HashSet<Sample>(stackTraces.size());
            for (Entry<StackTrace, int[]> e : stackTraces.entrySet()) {
                StackTrace stackTrace = e.getKey();
                int countCell[] = e.getValue();
                int count = countCell[0];
                Sample sample = new Sample(stackTrace, count);
                samples.add(sample);
            }
            return samples;
        }

        /**
         * Record an event in the thread history.
         */
        public void addThreadEvent(ThreadEvent event) {
            if (event == null) {
                throw new NullPointerException("event == null");
            }
            ThreadEvent old = threadIdToThreadEvent.put(event.threadId, event);
            switch (event.type) {
                case START:
                    if (old != null) {
                        throw new IllegalArgumentException("ThreadEvent already registered for id "
                                                           + event.threadId);
                    }
                    break;
                case END:
                    // Do not assert that the END_THREAD matches a
                    // START_THREAD unless in strict mode. While thhis
                    // hold true in the binary hprof BinaryHprofWriter
                    // produces, it is not true of hprof files created
                    // by the RI. However, if there is an event
                    // already registed for a thread id, it should be
                    // the matching start, not a duplicate end.
                    if (old != null && old.type == ThreadEventType.END) {
                        throw new IllegalArgumentException("Duplicate ThreadEvent.end for id "
                                                           + event.threadId);
                    }
                    break;
            }
            threadHistory.add(event);
        }

        /**
         * Record an stack trace and an associated int[] cell of
         * sample cound for the stack trace. The caller is allowed
         * retain a pointer to the cell to update the count. The
         * SamplingProfiler intentionally does not present a mutable
         * view of the count.
         */
        public void addStackTrace(StackTrace stackTrace, int[] countCell) {
            if (!threadIdToThreadEvent.containsKey(stackTrace.threadId)) {
                throw new IllegalArgumentException("Unknown thread id " + stackTrace.threadId);
            }
            int[] old = stackTraces.put(stackTrace, countCell);
            if (old != null) {
                throw new IllegalArgumentException("StackTrace already registered for id "
                                                   + stackTrace.stackTraceId + ":\n" + stackTrace);
            }
        }
    }

    public static interface HprofWriter {
        public void write() throws IOException;
    }

    public static final class AsciiHprofWriter implements HprofWriter {

        private final HprofData data;
        private final PrintWriter out;

        public AsciiHprofWriter(HprofData data, OutputStream outputStream) {
            this.data = data;
            this.out = new PrintWriter(outputStream);
        }

        public void write() throws IOException {
            for (HprofData.ThreadEvent e : data.getThreadHistory()) {
                out.println(e);
            }

            List<HprofData.Sample> samples
                    = new ArrayList<HprofData.Sample>(data.getSamples());
            Collections.sort(samples, SAMPLE_COMPARATOR);
            int total = 0;
            for (HprofData.Sample sample : samples) {
                HprofData.StackTrace stackTrace = sample.stackTrace;
                int count = sample.count;
                total += count;
                out.printf("TRACE %d: (thread=%d)\n",
                           stackTrace.stackTraceId,
                           stackTrace.threadId);
                for (StackTraceElement e : stackTrace.stackFrames) {
                    out.printf("\t%s\n", e);
                }
            }
            Date now = new Date(data.getStartMillis());
            // "CPU SAMPLES BEGIN (total = 826) Wed Jul 21 12:03:46 2010"
            out.printf("CPU SAMPLES BEGIN (total = %d) %ta %tb %td %tT %tY\n",
                       total, now, now, now, now, now);
            out.printf("rank   self  accum   count trace method\n");
            int rank = 0;
            double accum = 0;
            for (HprofData.Sample sample : samples) {
                rank++;
                HprofData.StackTrace stackTrace = sample.stackTrace;
                int count = sample.count;
                double self = (double)count/(double)total;
                accum += self;

                // "   1 65.62% 65.62%     542 300302 java.lang.Long.parseLong"
                out.printf("% 4d% 6.2f%%% 6.2f%% % 7d % 5d %s.%s\n",
                           rank, self*100, accum*100, count, stackTrace.stackTraceId,
                           stackTrace.stackFrames[0].getClassName(),
                           stackTrace.stackFrames[0].getMethodName());
            }
            out.printf("CPU SAMPLES END\n");
            out.flush();
        }

        private static final Comparator<HprofData.Sample> SAMPLE_COMPARATOR
                = new Comparator<HprofData.Sample>() {
            public int compare(HprofData.Sample s1, HprofData.Sample s2) {
                return s2.count - s1.count;
            }
        };
    }


    /**
     * Hprof binary format related constants shared between the
     * BinaryHprofReader and BinaryHprofWriter.
     */
    public static class BinaryHprof {
        /**
         * Currently code only supports 4 byte id size.
         */
        public static final int ID_SIZE = 4;

        public static enum Tag {

            STRING_IN_UTF8(0x01, -ID_SIZE),
            LOAD_CLASS(0x02, 4 + ID_SIZE + 4 + ID_SIZE),
            UNLOAD_CLASS(0x03, 4),
            STACK_FRAME(0x04, ID_SIZE + ID_SIZE + ID_SIZE + ID_SIZE + 4 + 4),
            STACK_TRACE(0x05, -(4 + 4 + 4)),
            ALLOC_SITES(0x06, -(2 + 4 + 4 + 4 + 8 + 8 + 4)),
            HEAP_SUMMARY(0x07, 4 + 4 + 8 + 8),
            START_THREAD(0x0a, 4 + ID_SIZE + 4 + ID_SIZE + ID_SIZE + ID_SIZE),
            END_THREAD(0x0b, 4),
            HEAP_DUMP(0x0c, -0),
            HEAP_DUMP_SEGMENT(0x1c, -0),
            HEAP_DUMP_END(0x2c, 0),
            CPU_SAMPLES(0x0d, -(4 + 4)),
            CONTROL_SETTINGS(0x0e, 4 + 2);

            public final byte tag;

            /**
             * Minimum size in bytes.
             */
            public final int minimumSize;

            /**
             * Maximum size in bytes. 0 mean no specific limit.
             */
            public final int maximumSize;

            private Tag(int tag, int size) {
                this.tag = (byte) tag;
                if (size > 0) {
                    // fixed size, max and min the same
                    this.minimumSize = size;
                    this.maximumSize = size;
                } else {
                    // only minimum bound
                    this.minimumSize = -size;
                    this.maximumSize = 0;
                }
            }

            private static final Map<Byte, Tag> BYTE_TO_TAG
                    = new HashMap<Byte, Tag>();

            static {
                for (Tag v : Tag.values()) {
                    BYTE_TO_TAG.put(v.tag, v);
                }
            }

            public static Tag get(byte tag) {
                return BYTE_TO_TAG.get(tag);
            }

            /**
             * Returns null if the actual size meets expectations, or a
             * String error message if not.
             */
            public String checkSize(int actual) {
                if (actual < minimumSize) {
                    return "expected a minimial record size of " + minimumSize + " for " + this
                            + " but received " + actual;
                }
                if (maximumSize == 0) {
                    return null;
                }
                if (actual > maximumSize) {
                    return "expected a maximum record size of " + maximumSize + " for " + this
                            + " but received " + actual;
                }
                return null;
            }
        }

        public static enum ControlSettings {
            ALLOC_TRACES(0x01),
            CPU_SAMPLING(0x02);

            public final int bitmask;

            private ControlSettings(int bitmask) {
                this.bitmask = bitmask;
            }
        }

    }

    public static final class BinaryHprofWriter implements HprofWriter {

        private int nextStringId = 1; // id 0 => null
        private int nextClassId = 1;
        private int nextStackFrameId = 1;
        private final Map<String, Integer> stringToId = new HashMap<String, Integer>();
        private final Map<String, Integer> classNameToId = new HashMap<String, Integer>();
        private final Map<StackTraceElement, Integer> stackFrameToId
                = new HashMap<StackTraceElement, Integer>();

        private final HprofData data;
        private final DataOutputStream out;

        public BinaryHprofWriter(HprofData data, OutputStream outputStream) {
            this.data = data;
            this.out = new DataOutputStream(outputStream);
        }

        public void write() throws IOException {
            try {
                writeHeader(data.getStartMillis());

                writeControlSettings(data.getFlags(), data.getDepth());

                for (HprofData.ThreadEvent event : data.getThreadHistory()) {
                    writeThreadEvent(event);
                }

                Set<HprofData.Sample> samples = data.getSamples();
                int total = 0;
                for (HprofData.Sample sample : samples) {
                    total += sample.count;
                    writeStackTrace(sample.stackTrace);
                }
                writeCpuSamples(total, samples);

            } finally {
                out.flush();
            }
        }

        private void writeHeader(long dumpTimeInMilliseconds) throws IOException {
            out.writeBytes("JAVA PROFILE 1.0.2");
            out.writeByte(0); // null terminated string
            out.writeInt(BinaryHprof.ID_SIZE);
            out.writeLong(dumpTimeInMilliseconds);
        }

        private void writeControlSettings(int flags, int depth) throws IOException {
            if (depth > Short.MAX_VALUE) {
                throw new IllegalArgumentException("depth too large for binary hprof: "
                                                   + depth + " > " + Short.MAX_VALUE);
            }
            writeRecordHeader(BinaryHprof.Tag.CONTROL_SETTINGS,
                              0,
                              BinaryHprof.Tag.CONTROL_SETTINGS.maximumSize);
            out.writeInt(flags);
            out.writeShort((short) depth);
        }

        private void writeThreadEvent(HprofData.ThreadEvent e) throws IOException {
            switch (e.type) {
                case START:
                    writeStartThread(e);
                    return;
                case END:
                    writeStopThread(e);
                    return;
            }
            throw new IllegalStateException(e.type.toString());
        }

        private void writeStartThread(HprofData.ThreadEvent e) throws IOException {
            int threadNameId = writeString(e.threadName);
            int groupNameId = writeString(e.groupName);
            int parentGroupNameId = writeString(e.parentGroupName);
            writeRecordHeader(BinaryHprof.Tag.START_THREAD,
                              0,
                              BinaryHprof.Tag.START_THREAD.maximumSize);
            out.writeInt(e.threadId);
            writeId(e.objectId);
            out.writeInt(0); // stack trace where thread was started unavailable
            writeId(threadNameId);
            writeId(groupNameId);
            writeId(parentGroupNameId);
        }

        private void writeStopThread(HprofData.ThreadEvent e) throws IOException {
            writeRecordHeader(BinaryHprof.Tag.END_THREAD,
                              0,
                              BinaryHprof.Tag.END_THREAD.maximumSize);
            out.writeInt(e.threadId);
        }

        private void writeRecordHeader(BinaryHprof.Tag hprofTag,
                                       int timeDeltaInMicroseconds,
                                       int recordLength) throws IOException {
            String error = hprofTag.checkSize(recordLength);
            if (error != null) {
                throw new AssertionError(error);
            }
            out.writeByte(hprofTag.tag);
            out.writeInt(timeDeltaInMicroseconds);
            out.writeInt(recordLength);
        }

        private void writeId(int id) throws IOException {
            out.writeInt(id);
        }

        /**
         * Ensures that a string has been writen to the out and
         * returns its ID. The ID of a null string is zero, and
         * doesn't actually result in any output. In a string has
         * already been written previously, the earlier ID will be
         * returned and no output will be written.
         */
        private int writeString(String string) throws IOException {
            if (string == null) {
                return 0;
            }
            Integer identifier = stringToId.get(string);
            if (identifier != null) {
                return identifier;
            }

            int id = nextStringId++;
            stringToId.put(string, id);

            byte[] bytes = string.getBytes("UTF-8");
            writeRecordHeader(BinaryHprof.Tag.STRING_IN_UTF8,
                              0,
                              BinaryHprof.ID_SIZE + bytes.length);
            out.writeInt(id);
            out.write(bytes, 0, bytes.length);

            return id;
        }

        private void writeCpuSamples(int totalSamples, Set<HprofData.Sample> samples)
                throws IOException {
            int samplesCount = samples.size();
            if (samplesCount == 0) {
                return;
            }
            writeRecordHeader(BinaryHprof.Tag.CPU_SAMPLES, 0, 4 + 4 + (samplesCount * (4 + 4)));
            out.writeInt(totalSamples);
            out.writeInt(samplesCount);
            for (HprofData.Sample sample : samples) {
                out.writeInt(sample.count);
                out.writeInt(sample.stackTrace.stackTraceId);
            }
        }

        private void writeStackTrace(HprofData.StackTrace stackTrace) throws IOException {
            int frames = stackTrace.stackFrames.length;
            int[] stackFrameIds = new int[frames];
            for (int i = 0; i < frames; i++) {
                stackFrameIds[i] = writeStackFrame(stackTrace.stackFrames[i]);
            }
            writeRecordHeader(BinaryHprof.Tag.STACK_TRACE,
                              0,
                              4 + 4 + 4 + (frames * BinaryHprof.ID_SIZE));
            out.writeInt(stackTrace.stackTraceId);
            out.writeInt(stackTrace.threadId);
            out.writeInt(frames);
            for (int stackFrameId : stackFrameIds) {
                writeId(stackFrameId);
            }
        }

        private int writeLoadClass(String className) throws IOException {
            Integer identifier = classNameToId.get(className);
            if (identifier != null) {
                return identifier;
            }
            int id = nextClassId++;
            classNameToId.put(className, id);

            int classNameId = writeString(className);
            writeRecordHeader(BinaryHprof.Tag.LOAD_CLASS,
                              0,
                              BinaryHprof.Tag.LOAD_CLASS.maximumSize);
            out.writeInt(id);
            writeId(0); // class object ID
            out.writeInt(0); // stack trace where class was loaded is unavailable
            writeId(classNameId);

            return id;
        }

        private int writeStackFrame(StackTraceElement stackFrame) throws IOException {
            Integer identifier = stackFrameToId.get(stackFrame);
            if (identifier != null) {
                return identifier;
            }

            int id = nextStackFrameId++;
            stackFrameToId.put(stackFrame, id);

            int classId = writeLoadClass(stackFrame.getClassName());
            int methodNameId = writeString(stackFrame.getMethodName());
            int sourceId = writeString(stackFrame.getFileName());
            writeRecordHeader(BinaryHprof.Tag.STACK_FRAME,
                              0,
                              BinaryHprof.Tag.STACK_FRAME.maximumSize);
            writeId(id);
            writeId(methodNameId);
            writeId(0); // method signature is unavailable from StackTraceElement
            writeId(sourceId);
            out.writeInt(classId);
            out.writeInt(stackFrame.getLineNumber());

            return id;
        }

        public void close() throws IOException {
            out.close();
        }
    }

    /**
     * <pre>   {@code
     * BinaryHprofReader reader = new BinaryHprofReader(new BufferedInputStream(inputStream));
     * reader.setStrict(false); // for RI compatability
     * reader.read();
     * inputStream.close();
     * reader.getVersion();
     * reader.getHprofData();
     * }</pre>
     */
    public static final class BinaryHprofReader {

        private static final boolean TRACE = false;

        private final DataInputStream in;

        /**
         * By default we try to strictly validate rules followed by
         * our HprofWriter. For example, every end thread is preceded
         * by a matching start thread.
         */
        private boolean strict = true;

        /**
         * version string from header after read has been performed,
         * otherwise null. nullness used to detect if callers try to
         * access data before read is called.
         */
        private String version;

        private final Map<HprofData.StackTrace, int[]> stackTraces
                = new HashMap<HprofData.StackTrace, int[]>();

        private final HprofData hprofData = new HprofData(stackTraces);

        private final Map<Integer, String> idToString = new HashMap<Integer, String>();
        private final Map<Integer, String> idToClassName = new HashMap<Integer, String>();
        private final Map<Integer, StackTraceElement> idToStackFrame
                = new HashMap<Integer, StackTraceElement>();
        private final Map<Integer, HprofData.StackTrace> idToStackTrace
                = new HashMap<Integer, HprofData.StackTrace>();

        /**
         * Creates a BinaryHprofReader around the specified {@code
         * inputStream}
         */
        public BinaryHprofReader(InputStream inputStream) throws IOException {
            this.in = new DataInputStream(inputStream);
        }

        public boolean getStrict () {
            return strict;
        }

        public void setStrict (boolean strict) {
            if (version != null) {
                throw new IllegalStateException("cannot set strict after read()");
            }
            this.strict = strict;
        }

        /**
         * throws IllegalStateException if read() has not been called.
         */
        private void checkRead() {
            if (version == null) {
                throw new IllegalStateException("data access before read()");
            }
        }

        public String getVersion() {
            checkRead();
            return version;
        }

        public HprofData getHprofData() {
            checkRead();
            return hprofData;
        }

        /**
         * Read the hprof header and records from the input
         */
        public void read() throws IOException {
            parseHeader();
            parseRecords();
        }

        private void parseHeader() throws IOException {
            if (TRACE) {
                System.out.println("hprofTag=HEADER");
            }
            parseVersion();
            parseIdSize();
            parseTime();
        }

        private void parseVersion() throws IOException {
            byte[] bytes = new byte[512];
            for (int i = 0; i < bytes.length; i++) {
                byte b = in.readByte();
                if (b == '\0') {
                    String version = new String(bytes, 0, i, "UTF-8");
                    if (TRACE) {
                        System.out.println("\tversion=" + version);
                    }
                    if (!version.startsWith("JAVA PROFILE ")) {
                        throw new MalformedHprofException("Unexpected version: " + version);
                    }
                    this.version = version;
                    return;
                }
                bytes[i] = b;
            }
            throw new MalformedHprofException("Could not find HPROF version");
        }

        private void parseIdSize() throws IOException {
            int idSize = in.readInt();
            if (TRACE) {
                System.out.println("\tidSize=" + idSize);
            }
            if (idSize != BinaryHprof.ID_SIZE) {
                throw new MalformedHprofException("Unsupported identifier size: " + idSize);
            }
        }

        private void parseTime() throws IOException {
            long time = in.readLong();
            if (TRACE) {
                System.out.println("\ttime=" + Long.toHexString(time) + " " + new Date(time));
            }
            hprofData.setStartMillis(time);
        }

        private void parseRecords() throws IOException {
            while (parseRecord()) {
                ;
            }
        }

        /**
         * Read and process the next record. Returns true if a
         * record was handled, false on EOF.
         */
        private boolean parseRecord() throws IOException {
            int tagOrEOF = in.read();
            if (tagOrEOF == -1) {
                return false;
            }
            byte tag = (byte) tagOrEOF;
            int timeDeltaInMicroseconds = in.readInt();
            int recordLength = in.readInt();
            BinaryHprof.Tag hprofTag = BinaryHprof.Tag.get(tag);
            if (TRACE) {
                System.out.println("hprofTag=" + hprofTag);
            }
            if (hprofTag == null) {
                skipRecord(hprofTag, recordLength);
                return true;
            }
            String error = hprofTag.checkSize(recordLength);
            if (error != null) {
                throw new MalformedHprofException(error);
            }
            switch (hprofTag) {
                case CONTROL_SETTINGS:
                    parseControlSettings();
                    return true;

                case STRING_IN_UTF8:
                    parseStringInUtf8(recordLength);
                    return true;

                case START_THREAD:
                    parseStartThread();
                    return true;
                case END_THREAD:
                    parseEndThread();
                    return true;

                case LOAD_CLASS:
                    parseLoadClass();
                    return true;
                case STACK_FRAME:
                    parseStackFrame();
                    return true;
                case STACK_TRACE:
                    parseStackTrace(recordLength);
                    return true;

                case CPU_SAMPLES:
                    parseCpuSamples(recordLength);
                    return true;

                case UNLOAD_CLASS:
                case ALLOC_SITES:
                case HEAP_SUMMARY:
                case HEAP_DUMP:
                case HEAP_DUMP_SEGMENT:
                case HEAP_DUMP_END:
                default:
                    skipRecord(hprofTag, recordLength);
                    return true;
            }
        }

        private void skipRecord(BinaryHprof.Tag hprofTag, long recordLength) throws IOException {
            if (TRACE) {
                System.out.println("\tskipping recordLength=" + recordLength);
            }
            long skipped = in.skip(recordLength);
            if (skipped != recordLength) {
                throw new EOFException("Expected to skip " + recordLength
                                       + " bytes but only skipped " + skipped + " bytes");
            }
        }

        private void parseControlSettings() throws IOException {
            int flags = in.readInt();
            short depth = in.readShort();
            if (TRACE) {
                System.out.println("\tflags=" + Integer.toHexString(flags));
                System.out.println("\tdepth=" + depth);
            }
            hprofData.setFlags(flags);
            hprofData.setDepth(depth);
        }

        private void parseStringInUtf8(int recordLength) throws IOException {
            int stringId = in.readInt();
            byte[] bytes = new byte[recordLength - BinaryHprof.ID_SIZE];
            in.read(bytes);
            String string = new String(bytes, "UTF-8");
            if (TRACE) {
                System.out.println("\tstring=" + string);
            }
            String old = idToString.put(stringId, string);
            if (old != null) {
                throw new MalformedHprofException("Duplicate string id: " + stringId);
            }
        }

        private void parseLoadClass() throws IOException {
            int classId = in.readInt();
            int classObjectId = readId();
            // serial number apparently not a stack trace id. (int vs ID)
            // we don't use this field.
            int stackTraceSerialNumber = in.readInt();
            String className = readString();
            if (TRACE) {
                System.out.println("\tclassId=" + classId);
                System.out.println("\tclassObjectId=" + classObjectId);
                System.out.println("\tstackTraceSerialNumber=" + stackTraceSerialNumber);
                System.out.println("\tclassName=" + className);
            }
            String old = idToClassName.put(classId, className);
            if (old != null) {
                throw new MalformedHprofException("Duplicate class id: " + classId);
            }
        }

        private int readId() throws IOException {
            return in.readInt();
        }

        private String readString() throws IOException {
            int id = readId();
            if (id == 0) {
                return null;
            }
            String string = idToString.get(id);
            if (string == null) {
                throw new MalformedHprofException("Unknown string id " + id);
            }
            return string;
        }

        private String readClass() throws IOException {
            int id = readId();
            String string = idToClassName.get(id);
            if (string == null) {
                throw new MalformedHprofException("Unknown class id " + id);
            }
            return string;
        }

        private void parseStartThread() throws IOException {
            int threadId = in.readInt();
            int objectId = readId();
            // stack trace where thread was created.
            // serial number apparently not a stack trace id. (int vs ID)
            // we don't use this field.
            int stackTraceSerialNumber = in.readInt();
            String threadName = readString();
            String groupName = readString();
            String parentGroupName = readString();
            if (TRACE) {
                System.out.println("\tthreadId=" + threadId);
                System.out.println("\tobjectId=" + objectId);
                System.out.println("\tstackTraceSerialNumber=" + stackTraceSerialNumber);
                System.out.println("\tthreadName=" + threadName);
                System.out.println("\tgroupName=" + groupName);
                System.out.println("\tparentGroupName=" + parentGroupName);
            }
            HprofData.ThreadEvent event
                    = HprofData.ThreadEvent.start(objectId, threadId,
                                                  threadName, groupName, parentGroupName);
            hprofData.addThreadEvent(event);
        }

        private void parseEndThread() throws IOException {
            int threadId = in.readInt();
            if (TRACE) {
                System.out.println("\tthreadId=" + threadId);
            }
            HprofData.ThreadEvent event = HprofData.ThreadEvent.end(threadId);
            hprofData.addThreadEvent(event);
        }

        private void parseStackFrame() throws IOException {
            int stackFrameId = readId();
            String methodName = readString();
            String methodSignature = readString();
            String file = readString();
            String className = readClass();
            int line = in.readInt();
            if (TRACE) {
                System.out.println("\tstackFrameId=" + stackFrameId);
                System.out.println("\tclassName=" + className);
                System.out.println("\tmethodName=" + methodName);
                System.out.println("\tmethodSignature=" + methodSignature);
                System.out.println("\tfile=" + file);
                System.out.println("\tline=" + line);
            }
            StackTraceElement stackFrame = new StackTraceElement(className, methodName, file, line);
            StackTraceElement old = idToStackFrame.put(stackFrameId, stackFrame);
            if (old != null) {
                throw new MalformedHprofException("Duplicate stack frame id: " + stackFrameId);
            }
        }

        private void parseStackTrace(int recordLength) throws IOException {
            int stackTraceId = in.readInt();
            int threadId = in.readInt();
            int frames = in.readInt();
            if (TRACE) {
                System.out.println("\tstackTraceId=" + stackTraceId);
                System.out.println("\tthreadId=" + threadId);
                System.out.println("\tframes=" + frames);
            }
            int expectedLength = 4 + 4 + 4 + (frames * BinaryHprof.ID_SIZE);
            if (recordLength != expectedLength) {
                throw new MalformedHprofException("Expected stack trace record of size "
                                                  + expectedLength
                                                  + " based on number of frames but header "
                                                  + "specified a length of  " + recordLength);
            }
            StackTraceElement[] stackFrames = new StackTraceElement[frames];
            for (int i = 0; i < frames; i++) {
                int stackFrameId = readId();
                StackTraceElement stackFrame = idToStackFrame.get(stackFrameId);
                if (TRACE) {
                    System.out.println("\tstackFrameId=" + stackFrameId);
                    System.out.println("\tstackFrame=" + stackFrame);
                }
                if (stackFrame == null) {
                    throw new MalformedHprofException("Unknown stack frame id " + stackFrameId);
                }
                stackFrames[i] = stackFrame;
            }

            HprofData.StackTrace stackTrace
                    = new HprofData.StackTrace(stackTraceId, threadId, stackFrames);
            if (strict) {
                hprofData.addStackTrace(stackTrace, new int[1]);
            } else {
                // The RI can have duplicate stacks, presumably they
                // have a minor race if two samples with the same
                // stack are taken around the same time. if we have a
                // duplicate, just skip adding it to hprofData, but
                // register it locally in idToStackFrame. if it seen
                // in CPU_SAMPLES, we will find a StackTrace is equal
                // to the first, so they will share a countCell.
                int[] countCell = stackTraces.get(stackTrace);
                if (countCell == null) {
                    hprofData.addStackTrace(stackTrace, new int[1]);
                }
            }

            HprofData.StackTrace old = idToStackTrace.put(stackTraceId, stackTrace);
            if (old != null) {
                throw new MalformedHprofException("Duplicate stack trace id: " + stackTraceId);
            }

        }

        private void parseCpuSamples(int recordLength) throws IOException {
            int totalSamples = in.readInt();
            int samplesCount = in.readInt();
            if (TRACE) {
                System.out.println("\ttotalSamples=" + totalSamples);
                System.out.println("\tsamplesCount=" + samplesCount);
            }
            int expectedLength = 4 + 4 + (samplesCount * (4 + 4));
            if (recordLength != expectedLength) {
                throw new MalformedHprofException("Expected CPU samples record of size "
                                                  + expectedLength
                                                  + " based on number of samples but header "
                                                  + "specified a length of  " + recordLength);
            }
            int total = 0;
            for (int i = 0; i < samplesCount; i++) {
                int count = in.readInt();
                int stackTraceId = in.readInt();
                if (TRACE) {
                    System.out.println("\tcount=" + count);
                    System.out.println("\tstackTraceId=" + stackTraceId);
                }
                HprofData.StackTrace stackTrace = idToStackTrace.get(stackTraceId);
                if (stackTrace == null) {
                    throw new MalformedHprofException("Unknown stack trace id " + stackTraceId);
                }
                if (count == 0) {
                    throw new MalformedHprofException("Zero sample count for stack trace "
                                                      + stackTrace);
                }
                int[] countCell = stackTraces.get(stackTrace);
                if (strict) {
                    if (countCell[0] != 0) {
                        throw new MalformedHprofException("Setting sample count of stack trace "
                                                          + stackTrace + " to " + count
                                                          + " found it was already initialized to "
                                                          + countCell[0]);
                    }
                } else {
                    // Coalesce counts from duplicate stack traces.
                    // For more on this, see comments in parseStackTrace.
                    count += countCell[0];
                }
                countCell[0] = count;
                total += count;
            }
            if (strict && totalSamples != total) {
                throw new MalformedHprofException("Expected a total of " + totalSamples
                                                  + " samples but saw " + total);
            }
        }
    }

    public static final class MalformedHprofException extends IOException {
        private MalformedHprofException(String message) {
            super(message);
        }
        private MalformedHprofException(String message, Throwable cause) {
            super(message, cause);
        }
        private MalformedHprofException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Run on device with:
     * adb shell dalvikvm 'dalvik.system.SamplingProfiler\$HprofBinaryToAscii'
     *
     * Run on host with:
     * java -classpath out/target/common/obj/JAVA_LIBRARIES/core_intermediates/classes.jar
     */
    public static final class HprofBinaryToAscii {

        public static void main(String[] args) {
            System.exit(convert(args) ? 0 : 1);
        }

        public static boolean convert(String[] args) {

            if (args.length != 1) {
                usage("binary hprof file argument expected");
                return false;
            }
            File file = new File(args[0]);
            if (!file.exists()) {
                usage("file " + file + " does not exist");
                return false;
            }

            HprofData hprofData;
            InputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(new FileInputStream(file));
                BinaryHprofReader reader = new BinaryHprofReader(inputStream);
                reader.setStrict(false);
                reader.read();
                hprofData = reader.getHprofData();
            } catch (IOException e) {
                System.out.println("Problem reading binary hprof data from "
                                   + file + ": " + e.getMessage());
                return false;
            } finally {
                IoUtils.closeQuietly(inputStream);
            }
            try {
                HprofWriter writer = new AsciiHprofWriter(hprofData, System.out);
                writer.write();
            } catch (IOException e) {
                System.out.println("Problem writing ASCII hprof data: " + e.getMessage());
                return false;
            }
            return true;
        }

        private static void usage(String error) {
            System.out.print("ERROR: ");
            System.out.println(error);
            System.out.println();
            System.out.println("usage: HprofBinaryToAscii <binary-hprof-file>");
            System.out.println();
            System.out.println("Reads a binary hprof file and print it in ASCII format");
        }
    }

    /**
     * Map of stack traces to a mutable sample count.
     */
    private final Map<HprofData.StackTrace, int[]> stackTraces
            = new HashMap<HprofData.StackTrace, int[]>();

    /**
     * Data collected by the sampling profiler
     */
    private final HprofData hprofData = new HprofData(stackTraces);

    /**
     * Timer that is used for the lifetime of the profiler
     */
    // note that dalvik/vm/Thread.c depends on this name
    private final Timer timer = new Timer("SamplingProfiler", true);

    /**
     * A sampler is created every time profiling starts and cleared
     * everytime profiling stops because once a {@code TimerTask} is
     * canceled it cannot be reused.
     */
    private TimerTask sampler;

    /**
     * The maximum number of {@code StackTraceElements} to retain in
     * each stack.
     */
    private final int depth;

    /**
     * The {@code ThreadSet} that identifies which threads to sample.
     */
    private final ThreadSet threadSet;

    /**
     * Create a sampling profiler that collects stacks with the
     * specified depth from the threads specified by the specified
     * thread collector.
     *
     * @param depth The maximum stack depth to retain for each sample
     * similar to the hprof option of the same name. Any stack deeper
     * than this will be truncated to this depth. A good starting
     * value is 4 although it is not uncommon to need to raise this to
     * get enough context to understand program behavior. While
     * programs with extensive recursion may require a high value for
     * depth, simply passing in a value for Integer.MAX_VALUE is not
     * advised because of the significant memory need to retain such
     * stacks and runtime overhead to compare stacks.
     */
    public SamplingProfiler(int depth, ThreadSet threadSet) {
        this.depth = depth;
        this.threadSet = threadSet;
        hprofData.setFlags(BinaryHprof.ControlSettings.CPU_SAMPLING.bitmask);
        hprofData.setDepth(depth);
    }

    /**
     * A ThreadSet specifies the set of threads to sample.
     */
    public static interface ThreadSet {
        /**
         * Returns an array containing the threads to be sampled. The
         * array may be longer than the number of threads to be
         * sampled, in which case the extra elements must be null.
         */
        public Thread[] threads();
    }

    /**
     * Returns a ThreadSet for a fixed set of threads that will not
     * vary at runtime. This has less overhead than a dynamically
     * calculated set, such as {@link newThreadGroupTheadSet}, which has
     * to enumerate the threads each time profiler wants to collect
     * samples.
     */
    public static ThreadSet newArrayThreadSet(Thread... threads) {
        return new ArrayThreadSet(threads);
    }

    /**
     * An ArrayThreadSet samples a fixed set of threads that does not
     * vary over the life of the profiler.
     */
    private static class ArrayThreadSet implements ThreadSet {
        private final Thread[] threads;
        public ArrayThreadSet(Thread... threads) {
            if (threads == null) {
                throw new NullPointerException("threads == null");
            }
            this.threads = threads;
        }
        public Thread[] threads() {
            return threads;
        }
    }

    /**
     * Returns a ThreadSet that is dynamically computed based on the
     * threads found in the specified ThreadGroup and that
     * ThreadGroup's children.
     */
    public static ThreadSet newThreadGroupTheadSet(ThreadGroup threadGroup) {
        return new ThreadGroupThreadSet(threadGroup);
    }

    /**
     * An ThreadGroupThreadSet sample the threads from the specified
     * ThreadGroup and the ThreadGroup's children
     */
    private static class ThreadGroupThreadSet implements ThreadSet {
        private final ThreadGroup threadGroup;
        private Thread[] threads;
        private int lastThread;

        public ThreadGroupThreadSet(ThreadGroup threadGroup) {
            if (threadGroup == null) {
                throw new NullPointerException("threadGroup == null");
            }
            this.threadGroup = threadGroup;
            resize();
        }

        private void resize() {
            int count = threadGroup.activeCount();
            // we can only tell if we had enough room for all active
            // threads if we actually are larger than the the number of
            // active threads. making it larger also leaves us room to
            // tolerate additional threads without resizing.
            threads = new Thread[count*2];
            lastThread = 0;
        }

        public Thread[] threads() {
            int threadCount;
            while (true) {
                threadCount = threadGroup.enumerate(threads);
                if (threadCount == threads.length) {
                    resize();
                } else {
                    break;
                }
            }
            if (threadCount < lastThread) {
                // avoid retaining pointers to threads that have ended
                Arrays.fill(threads, threadCount, lastThread, null);
            }
            lastThread = threadCount;
            return threads;
        }
    }

    /**
     * Starts profiler sampling at the specified rate.
     *
     * @param interval The number of milliseconds between samples
     */
    public void start(int interval) {
        if (interval < 1) {
            throw new IllegalArgumentException("interval < 1");
        }
        if (sampler != null) {
            throw new IllegalStateException("profiling already started");
        }
        sampler = new Sampler();
        hprofData.setStartMillis(System.currentTimeMillis());
        timer.scheduleAtFixedRate(sampler, 0, interval);
    }

    /**
     * Stops profiler sampling. It can be restarted with {@link
     * #start(int)} to continue sampling.
     */
    public void stop() {
        if (sampler == null) {
            return;
        }
        sampler.cancel();
        sampler = null;
    }

    /**
     * Shuts down profiling after which it can not be restarted. It is
     * important to shut down profiling when done to free resources
     * used by the profiler. Shutting down the profiler also stops the
     * profiling if that has not already been done.
     */
    public void shutdown() {
        stop();
        timer.cancel();
    }

    /**
     * Returns the hprof data accumulated by the profiler since it was
     * created. The profiler needs to be stopped, but not necessarily
     * shut down, in order to access the data. If the profiler is
     * restarted, there is no thread safe way to access the data.
     */
    public HprofData getHprofData() {
        if (sampler != null) {
            throw new IllegalStateException("cannot access hprof data while sampling");
        }
        return hprofData;
    }

    /**
     * The Sampler does the real work of the profiler.
     *
     * At every sample time, it asks the thread set for the set
     * of threads to sample. It maintains a history of thread creation
     * and death events based on changes observed to the threads
     * returned by the {@code ThreadSet}.
     *
     * For each thread to be sampled, a stack is collected and used to
     * update the set of collected samples. Stacks are truncated to a
     * maximum depth. There is no way to tell if a stack has been truncated.
     */
    private class Sampler extends TimerTask {

        private Thread timerThread;
        private Thread[] currentThreads = new Thread[0];

        /*
         *  Real hprof output examples don't start the thread and trace
         *  identifiers at one but seem to start at these arbitrary
         *  constants. It certainly seems useful to have relatively unique
         *  identifers when manual searching hprof output.
         */
        private int nextThreadId = 200001;
        private int nextStackTraceId = 300001;
        private int nextObjectId = 1;

        /**
         * Map of currently active threads to their identifiers. When
         * threads disappear they are removed and only referenced by their
         * identifiers to prevent retaining garbage threads.
         */
        private final Map<Thread, Integer> threadIds = new HashMap<Thread, Integer>();

        /**
         * Mutable StackTrace that is used for probing stackTraces Map
         * without allocating a StackTrace. If addStackTrace needs to
         * be thread safe, this would need to be reconsidered.
         */
        private final HprofData.StackTrace mutableStackTrace = new HprofData.StackTrace();

        public void run() {
            if (timerThread == null) {
                timerThread = Thread.currentThread();
            }

            // process thread creation and death first so that we
            // assign thread ids to any new threads before allocating
            // new stacks for them
            Thread[] newThreads = threadSet.threads();
            if (!Arrays.equals(currentThreads, newThreads)) {
                updateThreadHistory(currentThreads, newThreads);
                currentThreads = newThreads.clone();
            }

            for (Thread thread : currentThreads) {
                if (thread == null) {
                    break;
                }
                if (thread == timerThread) {
                    continue;
                }

                // TODO replace with a VMStack.getThreadStackTrace
                // variant to avoid allocating unneeded elements
                StackTraceElement[] stackFrames = thread.getStackTrace();
                if (stackFrames.length == 0) {
                    continue;
                }
                if (stackFrames.length > depth) {
                    stackFrames = Arrays.copyOfRange(stackFrames, 0, depth);
                }
                recordStackTrace(thread, stackFrames);
            }
        }

        /**
         * Record a new stack trace. The thread should have been
         * previously registered with addStartThread.
         */
        private void recordStackTrace(Thread thread, StackTraceElement[] stackFrames) {
            Integer threadId = threadIds.get(thread);
            if (threadId == null) {
                throw new IllegalArgumentException("Unknown thread " + thread);
            }
            mutableStackTrace.threadId = threadId;
            mutableStackTrace.stackFrames = stackFrames;

            int[] countCell = stackTraces.get(mutableStackTrace);
            if (countCell == null) {
                countCell = new int[1];
                HprofData.StackTrace stackTrace
                        = new HprofData.StackTrace(nextStackTraceId++, threadId, stackFrames);
                hprofData.addStackTrace(stackTrace, countCell);
            }
            countCell[0]++;
        }

        private void updateThreadHistory(Thread[] oldThreads, Thread[] newThreads) {
            // thread start/stop shouldn't happen too often and
            // these aren't too big, so hopefully this approach
            // won't be too slow...
            Set<Thread> n = new HashSet<Thread>(Arrays.asList(newThreads));
            Set<Thread> o = new HashSet<Thread>(Arrays.asList(oldThreads));

            // added = new-old
            Set<Thread> added = new HashSet<Thread>(n);
            added.removeAll(o);

            // removed = old-new
            Set<Thread> removed = new HashSet<Thread>(o);
            removed.removeAll(n);

            for (Thread thread : added) {
                if (thread == null) {
                    continue;
                }
                if (thread == timerThread) {
                    continue;
                }
                addStartThread(thread);
            }
            for (Thread thread : removed) {
                if (thread == null) {
                    continue;
                }
                if (thread == timerThread) {
                    continue;
                }
                addEndThread(thread);
            }
        }

        /**
         * Record that a newly noticed thread.
         */
        private void addStartThread(Thread thread) {
            if (thread == null) {
                throw new NullPointerException("thread == null");
            }
            int threadId = nextThreadId++;
            Integer old = threadIds.put(thread, threadId);
            if (old != null) {
                throw new IllegalArgumentException("Thread already registered as " + old);
            }

            String threadName = thread.getName();
            // group will become null when thread is terminated
            ThreadGroup group = thread.getThreadGroup();
            String groupName = group == null ? null : group.getName();
            ThreadGroup parentGroup = group == null ? null : group.getParent();
            String parentGroupName = parentGroup == null ? null : parentGroup.getName();

            HprofData.ThreadEvent event
                    = HprofData.ThreadEvent.start(nextObjectId++, threadId,
                                                  threadName, groupName, parentGroupName);
            hprofData.addThreadEvent(event);
        }

        /**
         * Record that a thread has disappeared.
         */
        private void addEndThread(Thread thread) {
            if (thread == null) {
                throw new NullPointerException("thread == null");
            }
            Integer threadId = threadIds.remove(thread);
            if (threadId == null) {
                throw new IllegalArgumentException("Unknown thread " + thread);
            }
            HprofData.ThreadEvent event = HprofData.ThreadEvent.end(threadId);
            hprofData.addThreadEvent(event);
        }
    }
}
