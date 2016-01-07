/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package dalvik.system;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by narayan on 1/7/16.
 */
public class BlockGuardTest extends TestCase {

    private BlockGuard.Policy oldPolicy;
    private RecordingPolicy recorder = new RecordingPolicy();

    @Override
    public void setUp() {
        oldPolicy = BlockGuard.getThreadPolicy();
        BlockGuard.setThreadPolicy(recorder);
    }

    @Override
    public void tearDown() {
        BlockGuard.setThreadPolicy(oldPolicy);
        recorder.clear();
    }

    public void testFile() throws Exception {
        File f = File.createTempFile("foo", "bar");
        recorder.expectAndClear("onReadFromDisk", "onWriteToDisk");

        f.getAbsolutePath();
        f.getParentFile();
        f.getName();
        f.getParent();
        f.getPath();
        f.isAbsolute();
        recorder.expectNoViolations();

        f.mkdir();
        recorder.expectAndClear("onWriteToDisk");

        f.listFiles();
        recorder.expectAndClear("onReadFromDisk");

        f.list();
        recorder.expectAndClear("onReadFromDisk");

        f.length();
        recorder.expectAndClear("onReadFromDisk");

        f.lastModified();
        recorder.expectAndClear("onReadFromDisk");

        f.canExecute();
        recorder.expectAndClear("onReadFromDisk");

        f.canRead();
        recorder.expectAndClear("onReadFromDisk");

        f.canWrite();
        recorder.expectAndClear("onReadFromDisk");

        f.isFile();
        recorder.expectAndClear("onReadFromDisk");

        f.isDirectory();
        recorder.expectAndClear("onReadFromDisk");

        f.setExecutable(true, false);
        recorder.expectAndClear("onWriteToDisk");

        f.setReadable(true, false);
        recorder.expectAndClear("onWriteToDisk");

        f.setWritable(true, false);
        recorder.expectAndClear("onWriteToDisk");

        f.delete();
        recorder.expectAndClear("onWriteToDisk");
    }

    public void testFileInputStream() throws Exception {
        File f = new File("/proc/version");
        recorder.clear();

        FileInputStream fis = new FileInputStream(f);
        recorder.expectAndClear("onReadFromDisk");

        fis.read(new byte[4],0, 4);
        recorder.expectAndClear("onReadFromDisk");

        fis.read();
        recorder.expectAndClear("onReadFromDisk");

        fis.skip(1);
        recorder.expectAndClear("onReadFromDisk");

        fis.close();
    }

    public void testFileOutputStream() throws Exception {
        File f = File.createTempFile("foo", "bar");
        recorder.clear();

        FileOutputStream fos = new FileOutputStream(f);
        recorder.expectAndClear("onWriteToDisk");

        fos.write(new byte[3]);
        recorder.expectAndClear("onWriteToDisk");

        fos.write(4);
        recorder.expectAndClear("onWriteToDisk");

        fos.flush();
        recorder.expectNoViolations();

        fos.close();
        recorder.expectNoViolations();
    }


    public static class RecordingPolicy implements BlockGuard.Policy {
        private final List<String> violations = new ArrayList<>();

        @Override
        public void onWriteToDisk() {
            addViolation("onWriteToDisk");
        }

        @Override
        public void onReadFromDisk() {
            addViolation("onReadFromDisk");
        }

        @Override
        public void onNetwork() {
            addViolation("onNetwork");
        }

        private void addViolation(String type) {
            StackTraceElement[] threadTrace = Thread.currentThread().getStackTrace();

            final StackTraceElement violator = threadTrace[4];
            violations.add(type + " [caller= " + violator.getMethodName() + "]");
        }

        public void clear() {
            violations.clear();
        }

        public void expectNoViolations() {
            if (violations.size() != 0) {
                throw new AssertionError("Expected 0 violations but found " + violations.size());
            }
        }

        public void expectAndClear(String... expected) {
            if (expected.length != violations.size()) {
                throw new AssertionError("Expected " + expected.length + " violations but found "
                        + violations.size());
            }

            for (int i = 0; i < expected.length; ++i) {
                if (!violations.get(i).startsWith(expected[i])) {
                    throw new AssertionError("Expected: " + expected[i] + " but was "
                            + violations.get(i));
                }
            }

            clear();
        }

        @Override
        public int getPolicyMask() {
            return 0;
        }
    }
}
