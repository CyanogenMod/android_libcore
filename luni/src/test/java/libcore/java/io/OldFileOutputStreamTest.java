/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package libcore.java.io;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class OldFileOutputStreamTest extends junit.framework.TestCase {

    public String fileName;

    FileOutputStream fos;

    FileInputStream fis;

    File f;

    String tmpDirName = System.getProperty("java.io.tmpdir");

    File tmpDir = new File(tmpDirName);

    public String fileString = "Test_All_Tests\nTest_java_io_BufferedInputStream\nTest_java_io_BufferedOutputStream\nTest_java_io_ByteArrayInputStream\nTest_java_io_ByteArrayOutputStream\nTest_java_io_DataInputStream\nTest_java_io_File\nTest_java_io_FileDescriptor\nTest_java_io_FileInputStream\nTest_java_io_FileNotFoundException\nTest_FileOutputStream\nTest_java_io_FilterInputStream\nTest_java_io_FilterOutputStream\nTest_java_io_InputStream\nTest_java_io_IOException\nTest_java_io_OutputStream\nTest_java_io_PrintStream\nTest_java_io_RandomAccessFile\nTest_java_io_SyncFailedException\nTest_java_lang_AbstractMethodError\nTest_java_lang_ArithmeticException\nTest_java_lang_ArrayIndexOutOfBoundsException\nTest_java_lang_ArrayStoreException\nTest_java_lang_Boolean\nTest_java_lang_Byte\nTest_java_lang_Character\nTest_java_lang_Class\nTest_java_lang_ClassCastException\nTest_java_lang_ClassCircularityError\nTest_java_lang_ClassFormatError\nTest_java_lang_ClassLoader\nTest_java_lang_ClassNotFoundException\nTest_java_lang_CloneNotSupportedException\nTest_java_lang_Double\nTest_java_lang_Error\nTest_java_lang_Exception\nTest_java_lang_ExceptionInInitializerError\nTest_java_lang_Float\nTest_java_lang_IllegalAccessError\nTest_java_lang_IllegalAccessException\nTest_java_lang_IllegalArgumentException\nTest_java_lang_IllegalMonitorStateException\nTest_java_lang_IllegalThreadStateException\nTest_java_lang_IncompatibleClassChangeError\nTest_java_lang_IndexOutOfBoundsException\nTest_java_lang_InstantiationError\nTest_java_lang_InstantiationException\nTest_java_lang_Integer\nTest_java_lang_InternalError\nTest_java_lang_InterruptedException\nTest_java_lang_LinkageError\nTest_java_lang_Long\nTest_java_lang_Math\nTest_java_lang_NegativeArraySizeException\nTest_java_lang_NoClassDefFoundError\nTest_java_lang_NoSuchFieldError\nTest_java_lang_NoSuchMethodError\nTest_java_lang_NullPointerException\nTest_java_lang_Number\nTest_java_lang_NumberFormatException\nTest_java_lang_Object\nTest_java_lang_OutOfMemoryError\nTest_java_lang_RuntimeException\nTest_java_lang_SecurityManager\nTest_java_lang_Short\nTest_java_lang_StackOverflowError\nTest_java_lang_String\nTest_java_lang_StringBuffer\nTest_java_lang_StringIndexOutOfBoundsException\nTest_java_lang_System\nTest_java_lang_Thread\nTest_java_lang_ThreadDeath\nTest_java_lang_ThreadGroup\nTest_java_lang_Throwable\nTest_java_lang_UnknownError\nTest_java_lang_UnsatisfiedLinkError\nTest_java_lang_VerifyError\nTest_java_lang_VirtualMachineError\nTest_java_lang_vm_Image\nTest_java_lang_vm_MemorySegment\nTest_java_lang_vm_ROMStoreException\nTest_java_lang_vm_VM\nTest_java_lang_Void\nTest_java_net_BindException\nTest_java_net_ConnectException\nTest_java_net_DatagramPacket\nTest_java_net_DatagramSocket\nTest_java_net_DatagramSocketImpl\nTest_java_net_InetAddress\nTest_java_net_NoRouteToHostException\nTest_java_net_PlainDatagramSocketImpl\nTest_java_net_PlainSocketImpl\nTest_java_net_Socket\nTest_java_net_SocketException\nTest_java_net_SocketImpl\nTest_java_net_SocketInputStream\nTest_java_net_SocketOutputStream\nTest_java_net_UnknownHostException\nTest_java_util_ArrayEnumerator\nTest_java_util_Date\nTest_java_util_EventObject\nTest_java_util_HashEnumerator\nTest_java_util_Hashtable\nTest_java_util_Properties\nTest_java_util_ResourceBundle\nTest_java_util_tm\nTest_java_util_Vector\n";

    public void test_ConstructorLjava_io_File() throws Exception {
        try {
            fos = new FileOutputStream(tmpDir);
            fail("Test 1: FileNotFoundException expected.");
        } catch (FileNotFoundException e) {
            // Expected.
        }

        f = new File(fileName = System.getProperty("java.io.tmpdir"), "fos.tst");
        fos = new FileOutputStream(f);
    }

    public void test_ConstructorLjava_io_FileZ() throws Exception {
        try {
            fos = new FileOutputStream(tmpDir, false);
            fail("Test 1: FileNotFoundException expected.");
        } catch (FileNotFoundException e) {
            // Expected.
        }

        f = new File(tmpDirName, "fos.tst");
        fos = new FileOutputStream(f, false);
        fos.write("FZ1".getBytes(), 0, 3);
        fos.close();
        // Append data to existing file
        fos = new FileOutputStream(f, true);
        fos.write(fileString.getBytes());
        fos.close();
        byte[] buf = new byte[fileString.length() + 3];
        fis = new FileInputStream(f);
        fis.read(buf, 0, buf.length);
        assertTrue("Test 2: Failed to create appending stream.", new String(buf, 0,
                buf.length).equals("FZ1" + fileString));
        fis.close();

        // Check that the existing file is overwritten
        fos = new FileOutputStream(f, false);
        fos.write("FZ2".getBytes(), 0, 3);
        fos.close();
        fis = new FileInputStream(f);
        int bytesRead = fis.read(buf, 0, buf.length);
        assertTrue("Test 3: Failed to overwrite stream.", new String(buf, 0,
                bytesRead).equals("FZ2"));
    }

    public void test_ConstructorLjava_lang_String() throws Exception {
        try {
            fos = new FileOutputStream(tmpDirName);
            fail("Test 1: FileNotFoundException expected.");
        } catch (FileNotFoundException e) {
            // Expected.
        }
    }

    public void test_ConstructorLjava_lang_StringZ() throws Exception {
        try {
            fos = new FileOutputStream(tmpDirName, true);
            fail("Test 1: FileNotFoundException expected.");
        } catch (FileNotFoundException e) {
            // Expected.
        }

        f = new File(tmpDirName, "fos.tst");
        fos = new FileOutputStream(f.getPath(), false);
        fos.write("HI".getBytes(), 0, 2);
        fos.close();
        // Append data to existing file
        fos = new FileOutputStream(f.getPath(), true);
        fos.write(fileString.getBytes());
        fos.close();
        byte[] buf = new byte[fileString.length() + 2];
        fis = new FileInputStream(f.getPath());
        fis.read(buf, 0, buf.length);
        assertTrue("Failed to create appending stream", new String(buf, 0,
                buf.length).equals("HI" + fileString));
        fis.close();

        // Check that the existing file is overwritten
        fos = new FileOutputStream(f.getPath(), false);
        fos.write("HI".getBytes(), 0, 2);
        fos.close();
        fis = new FileInputStream(f.getPath());
        int bytesRead = fis.read(buf, 0, buf.length);
        assertTrue("Failed to overwrite stream", new String(buf, 0,
                bytesRead).equals("HI"));
    }

    public void test_write$B() throws Exception {
        // Test for method void java.io.FileOutputStream.write(byte [])
        f = new File(System.getProperty("java.io.tmpdir"), "output.tst");
        fos = new FileOutputStream(f.getPath());
        fos.write(fileString.getBytes());
        fos.close();
        try {
            fos.write(fileString.getBytes());
            fail("Test 1: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }

        fis = new FileInputStream(f.getPath());
        byte rbytes[] = new byte[4000];
        fis.read(rbytes, 0, fileString.length());
        assertTrue("Test 2: Incorrect string written or read.",
                new String(rbytes, 0, fileString.length()).equals(fileString));
    }

    public void test_writeI() throws IOException {
        // Test for method void java.io.FileOutputStream.write(int)
        f = new File(System.getProperty("java.io.tmpdir"), "output.tst");
        fos = new FileOutputStream(f.getPath());
        fos.write('t');
        fos.close();
        try {
            fos.write(42);
            fail("Test: IOException expected.");
        } catch (IOException e) {
            // Expected.
        }

        fis = new FileInputStream(f.getPath());
        assertEquals("Test 1: Incorrect char written or read.",
                't', fis.read());
    }

    public void test_write$BII3() {
        try {
            new FileOutputStream(new FileDescriptor()).write(new byte[1], 0, 0);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    public void test_getChannel() throws Exception {
        // Make sure that system properties are set correctly
        if (tmpDir == null) {
            throw new Exception("System property java.io.tmpdir not defined.");
        }
        File tmpfile = File.createTempFile("FileOutputStream", "tmp");
        tmpfile.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(tmpfile);
        byte[] b = new byte[10];
        for (int i = 10; i < b.length; i++) {
            b[i] = (byte) i;
        }
        fos.write(b);
        fos.flush();
        fos.close();
        FileOutputStream f = new FileOutputStream(tmpfile, true);
        assertEquals(10, f.getChannel().position());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (f != null)
            f.delete();
        if (fis != null)
            fis.close();
        if (fos != null)
            fos.close();
    }
}
