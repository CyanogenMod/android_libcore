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

package libcore.java.lang.reflect;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

public final class ClassLoaderReflectionTest extends TestCase {

    private static final String prefix = ClassLoaderReflectionTest.class.getName() + "$";

    public void testLoadOneClassInTwoClassLoadersSimultaneously() throws Exception {
        ClassLoader loader = twoCopiesClassLoader(prefix);
        Class<?> aClass = loader.loadClass(prefix + "A");
        assertEquals(aClass.getName(), A.class.getName());
        assertNotSame(aClass, A.class);
    }

    public void testGetFieldSameClassLoader() throws Exception {
        assertEquals(A.class, AList.class.getDeclaredField("field").getType());
    }

    public void testGetFieldDifferentClassLoader() throws Exception {
        ClassLoader loader = twoCopiesClassLoader(prefix);
        Class<?> aClass = loader.loadClass(prefix + "A");
        Class<?> aListClass = loader.loadClass(prefix + "AList");
        assertEquals(aClass, aListClass.getDeclaredField("field").getType());
    }

    public void testGetGenericSuperclassSameClassLoader() throws Exception {
        ParameterizedType type = (ParameterizedType) AList.class.getGenericSuperclass();
        assertEquals(ArrayList.class, type.getRawType());
        assertEquals(Arrays.<Type>asList(A.class), Arrays.asList(type.getActualTypeArguments()));
    }

    /**
     * http://code.google.com/p/android/issues/detail?id=10111
     */
    public void testGetGenericSuperclassDifferentClassLoader() throws Exception {
        ClassLoader loader = twoCopiesClassLoader(prefix);
        Class<?> aClass = loader.loadClass(prefix + "A");
        Class<?> aListClass = loader.loadClass(prefix + "AList");
        ParameterizedType type = (ParameterizedType) aListClass.getGenericSuperclass();
        assertEquals(ArrayList.class, type.getRawType());
        assertEquals(Arrays.<Type>asList(aClass), Arrays.asList(type.getActualTypeArguments()));
    }

    /**
     * http://code.google.com/p/android/issues/detail?id=6636
     */
    public void testGetGenericSuperclassToString()
            throws ClassNotFoundException, IOException, InterruptedException {
        assertEquals("java.util.ArrayList<libcore.java.lang.reflect.ClassLoaderReflectionTest$A>",
                AList.class.getGenericSuperclass().toString());
    }

    static class A {}
    static class AList extends ArrayList<A> {
        A field;
    }

    /**
     * Returns a class loader that permits multiple copies of the same class to
     * be loaded into the same VM at the same time. This loads classes using the
     * same classpath as the application class loader.
     *
     * @param prefix the prefix of classes that can be loaded by both the
     *     returned class loader and the application class loader.
     */
    private ClassLoader twoCopiesClassLoader(final String prefix)
            throws IOException, InterruptedException {

        /*
         * To load two copies of a given class in the VM, we end up creating two
         * new class loaders: a bridge class loader and a leaf class loader.
         *
         * The bridge class loader is a child of the application class loader.
         * It never loads any classes. All it does is decide when to delegate to
         * the application class loader (which has a copy of everything) and
         * when to fail.
         *
         * The leaf class loader is a child of the bridge class loader. It
         * uses the same classpath as the application class loader. It loads
         * anything that its parent failed on.
         */

        ClassLoader bridge = new ClassLoader() {
            @Override protected Class<?> loadClass(String className, boolean resolve)
                    throws ClassNotFoundException {
                if (className.startsWith(prefix)) {
                    /* throwing will cause the child class loader to load the class. */
                    throw new ClassNotFoundException();
                } else {
                    return super.loadClass(className, resolve);
                }
            }
        };

        try {
            // first try to create a PathClassLoader for a dalvik VM...
            String classPath = System.getProperty("java.class.path");
            return (ClassLoader) Class.forName("dalvik.system.PathClassLoader")
                    .getConstructor(String.class, ClassLoader.class)
                    .newInstance(classPath, bridge);
        } catch (Exception ignored) {
        }

        // fall back to a URLClassLoader on a JVM
        List<URL> classpath = new ArrayList<URL>();
        classpath.addAll(classpathToUrls("java.class.path"));
        classpath.addAll(classpathToUrls("sun.boot.class.path"));
        return new URLClassLoader(classpath.toArray(new URL[classpath.size()]), bridge);
    }

    private List<URL> classpathToUrls(String propertyName) throws MalformedURLException {
        String classpath = System.getProperty(propertyName);
        List<URL> result = new ArrayList<URL>();
        for (String pathElement : classpath.split(File.pathSeparator)) {
            result.add(new File(pathElement).toURI().toURL());
        }
        return result;
    }
}
