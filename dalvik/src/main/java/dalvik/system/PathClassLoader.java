/*
 * Copyright (C) 2007 The Android Open Source Project
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

/**
 * Provides a simple {@link ClassLoader} implementation that operates on a list
 * of files and directories in the local file system, but does not attempt to
 * load classes from the network. Android uses this class for its system class
 * loader and for its application class loader(s).
 */
public class PathClassLoader extends BaseDexClassLoader {
    /**
     * Creates a {@code PathClassLoader} that operates on a given list of files
     * and directories. This method is equivalent to calling
     * {@link #PathClassLoader(String, String, ClassLoader)} with a
     * {@code null} value for the second argument (see description there).
     *
     * @param dexPath the list of jar/apk files containing classes and
     * resources, delimited by {@code File.pathSeparator}, which
     * defaults to {@code ":"} on Android
     * @param parent the parent class loader
     */
    public PathClassLoader(String dexPath, ClassLoader parent) {
        super(dexPath, null, false, null, null, parent);
    }

    /**
     * Creates a {@code PathClassLoader} that operates on two given
     * lists of files and directories. The entries of the first list
     * should be one of the following:
     *
     * <ul>
     * <li>JAR/ZIP/APK files, possibly containing a "classes.dex" file as
     * well as arbitrary resources.
     * <li>Raw ".dex" files (not inside a zip file).
     * </ul>
     *
     * The entries of the second list should be directories containing
     * native library files.
     *
     * @param dexPath the list of jar/apk files containing classes and
     * resources, delimited by {@code File.pathSeparator}, which
     * defaults to {@code ":"} on Android
     * @param librarySearchPath the list of directories containing native
     * libraries, delimited by {@code File.pathSeparator}; may be
     * {@code null}
     * @param parent the parent class loader
     *
     * This method will be deprecated in the next release
     */
    public PathClassLoader(String dexPath, String librarySearchPath, ClassLoader parent) {
        super(dexPath, null, false, librarySearchPath, null, parent);
    }

    /**
     * Creates a {@code PathClassLoader} that operates on two given
     * lists of files and directories. The entries of the first list
     * should be one of the following:
     *
     * <ul>
     * <li>JAR/ZIP/APK files, possibly containing a "classes.dex" file as
     * well as arbitrary resources.
     * <li>Raw ".dex" files (not inside a zip file).
     * </ul>
     *
     * The entries of the second list should be directories containing
     * native library files.
     *
     * @param dexPath the list of jar/apk files containing classes and
     * resources, delimited by {@code File.pathSeparator}, which
     * defaults to {@code ":"} on Android
     * @param isSharedNamespace whether this classloader should use the shared linker
     * namespace. If the shared linker namespace is used, the classloader will have
     * access to all native libraries loaded by the platform. This should be limited
     * to the classloaders used by the bundled apps - bundled apps are part of the
     * platform
     * @param librarySearchPath the list of directories containing native
     * libraries, delimited by {@code File.pathSeparator}; may be
     * {@code null}
     * @param libraryPermittedPath allowing to open native libraries under
     * directories in this list. The list is delimited by {@code File.pathSeparator}.
     * Note that the classloader is implicitly allowed to open libraries from the
     * directories on librarySearchPath. Directories from this list are NOT used
     * to search for the native library; may be {@code null}
     * @param parent the parent class loader
     *
     * @hide
     */
    public PathClassLoader(String dexPath, boolean isSharedNamespace, String librarySearchPath,
            String libraryPermittedPath, ClassLoader parent) {
        super(dexPath, null, isSharedNamespace, librarySearchPath, libraryPermittedPath, parent);
    }
}
