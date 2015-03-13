/*
 * Copyright (C) 2015 The Android Open Source Project
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
package libcore.tzdata.update;

import junit.framework.TestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import libcore.tzdata.update.tools.TzDataBundleBuilder;

/**
 * Tests for {@link libcore.tzdata.update.TzDataBundleInstaller}.
 */
public class TzDataBundleInstallerTest extends TestCase {

    private static final File SYSTEM_ZONE_INFO_FILE = new File("/system/usr/share/zoneinfo/tzdata");

    private TzDataBundleInstaller installer;
    private File tempDir;
    private File testInstallDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        tempDir = createDirectory("tempDir");
        testInstallDir =  createDirectory("testInstall");
        installer = new TzDataBundleInstaller("TzDataBundleInstallerTest", testInstallDir);
    }

    private static File createDirectory(String prefix) throws IOException {
        File dir = File.createTempFile(prefix, "");
        assertTrue(dir.delete());
        assertTrue(dir.mkdir());
        return dir;
    }

    @Override
    public void tearDown() throws Exception {
        if (testInstallDir.exists()) {
            FileUtils.deleteRecursive(testInstallDir);
        }
        if (tempDir.exists()) {
            FileUtils.deleteRecursive(tempDir);
        }
        super.tearDown();
    }

    /** Tests the first update on a device */
    public void testSuccessfulFirstUpdate() throws Exception {
        ConfigBundle tzData = createValidTzDataBundle("2030a");

        assertTrue(install(tzData));
        assertTzDataInstalled(tzData);
    }

    /**
     * Tests an update on a device when there is a prior update already applied.
     */
    public void testSuccessfulFollowOnUpdate() throws Exception {
        ConfigBundle tzData1 = createValidTzDataBundle("2030a");
        assertTrue(install(tzData1));
        assertTzDataInstalled(tzData1);

        ConfigBundle tzData2 = createValidTzDataBundle("2030b");
        assertTrue(install(tzData2));
        assertTzDataInstalled(tzData2);
    }


    /** Tests that a bundle with a missing file will not update the content. */
    public void testMissingRequiredBundleFile() throws Exception {
        ConfigBundle installedConfigBundle = createValidTzDataBundle("2030a");
        assertTrue(install(installedConfigBundle));
        assertTzDataInstalled(installedConfigBundle);

        ConfigBundle incompleteUpdate =
                createValidTzDataBundleBuilder("2030b").clearBionicTzData().buildUnvalidated();
        assertFalse(install(incompleteUpdate));
        assertTzDataInstalled(installedConfigBundle);
    }

    /**
     * Tests that an update will be unpacked even if there is a partial update from a previous run.
     */
    public void testInstallWithWorkingDir() throws Exception {
        File workingDir = new File(testInstallDir, TzDataBundleInstaller.WORKING_DIR_NAME);
        assertTrue(workingDir.mkdir());
        createFile(new File(workingDir, "myFile"));

        ConfigBundle tzData = createValidTzDataBundle("2030a");
        assertTrue(install(tzData));
        assertTzDataInstalled(tzData);
    }

    /**
     * Tests that a bundle with a checksum entry that references a missing file will not update the
     * content.
     */
    public void testChecksumBundleEntry_fileMissing() throws Exception {
        ConfigBundle badUpdate =
                createValidTzDataBundleBuilder("2030b")
                        .addChecksum("/fileDoesNotExist", 1234)
                        .build();
        assertFalse(install(badUpdate));
        assertNoContentInstalled();
    }

    /**
     * Tests that a bundle with a checksum entry with a bad checksum will not update the
     * content.
     */
    public void testChecksumBundleEntry_incorrectChecksum() throws Exception {
        File fileToChecksum = SYSTEM_ZONE_INFO_FILE;
        long badChecksum = FileUtils.calculateChecksum(fileToChecksum) + 1;
        ConfigBundle badUpdate =
                createValidTzDataBundleBuilder("2030b")
                        .clearChecksumEntries()
                        .addChecksum(fileToChecksum.getPath(), badChecksum)
                        .build();
        assertFalse(install(badUpdate));
        assertNoContentInstalled();
    }

    private boolean install(ConfigBundle configBundle) throws Exception {
        return installer.install(configBundle.getBundleBytes());
    }

    private ConfigBundle createValidTzDataBundle(String tzDataVersion)
            throws IOException {
        return createValidTzDataBundleBuilder(tzDataVersion).build();
    }

    private TzDataBundleBuilder createValidTzDataBundleBuilder(String tzDataVersion)
            throws IOException {

        // The file to include in the installation-time checksum check.
        File fileToChecksum = SYSTEM_ZONE_INFO_FILE;
        long checksum = FileUtils.calculateChecksum(fileToChecksum);

        File bionicTzData = new File(tempDir, "zoneinfo");
        createFile(bionicTzData);

        File icuData = new File(tempDir, "icudata");
        createFile(icuData);

        return new TzDataBundleBuilder()
                .addChecksum(fileToChecksum.getPath(), checksum)
                .setTzDataVersion(tzDataVersion)
                .addBionicTzData(bionicTzData)
                .addIcuTzData(icuData);
    }

    private void assertTzDataInstalled(ConfigBundle expectedTzData) throws Exception {
        assertTrue(testInstallDir.exists());

        File currentTzDataDir = new File(testInstallDir, TzDataBundleInstaller.CURRENT_TZ_DATA_DIR_NAME);
        assertTrue(currentTzDataDir.exists());

        File checksumFile = new File(currentTzDataDir, ConfigBundle.CHECKSUMS_FILE_NAME);
        assertTrue(checksumFile.exists());

        File versionFile = new File(currentTzDataDir,
                ConfigBundle.TZ_DATA_VERSION_FILE_NAME);
        assertTrue(versionFile.exists());

        File bionicFile = new File(currentTzDataDir, ConfigBundle.ZONEINFO_FILE_NAME);
        assertTrue(bionicFile.exists());

        File icuFile = new File(currentTzDataDir, ConfigBundle.ICU_DATA_FILE_NAME);
        assertTrue(icuFile.exists());

        // Also check no working directory is left lying around.
        File workingDir = new File(testInstallDir, TzDataBundleInstaller.WORKING_DIR_NAME);
        assertFalse(workingDir.exists());
    }

    private void assertNoContentInstalled() {
        File currentTzDataDir = new File(testInstallDir, TzDataBundleInstaller.CURRENT_TZ_DATA_DIR_NAME);
        assertFalse(currentTzDataDir.exists());

        // Also check no working directories are left lying around.
        File workingDir = new File(testInstallDir, TzDataBundleInstaller.WORKING_DIR_NAME);
        assertFalse(workingDir.exists());

        File oldDataDir = new File(testInstallDir, TzDataBundleInstaller.OLD_TZ_DATA_DIR_NAME);
        assertFalse(oldDataDir.exists());
    }

    private static void createFile(File file) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write('a');
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
