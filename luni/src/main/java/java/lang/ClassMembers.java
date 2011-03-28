/*
 * Copyright (C) 2008 The Android Open Source Project
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

package java.lang;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libcore.util.EmptyArray;

/**
 * Reflection support.
 */
final class ClassMembers {

    /**
     * Returns public methods defined by {@code clazz}, its superclasses and all
     * implemented interfaces, not including overridden methods. This method
     * performs no security checks.
     */
    public static Method[] getMethods(Class<?> clazz) {
        List<Method> allMethods = new ArrayList<Method>();
        getMethodsRecursive(clazz, allMethods);

        /*
         * Remove methods defined by multiple types, preferring to keep methods
         * declared by derived types.
         */
        Collections.sort(allMethods, Method.ORDER_BY_SIGNATURE);
        List<Method> result = new ArrayList<Method>(allMethods.size());
        Method previous = null;
        for (Method method : allMethods) {
            if (previous != null
                    && Method.ORDER_BY_SIGNATURE.compare(method, previous) == 0
                    && method.getDeclaringClass() != previous.getDeclaringClass()) {
                continue;
            }
            result.add(method);
            previous = method;
        }
        return result.toArray(new Method[result.size()]);
    }

    /**
     * Populates {@code result} with public methods defined by {@code clazz}, its
     * superclasses, and all implemented interfaces, including overridden methods.
     * This method performs no security checks.
     */
    private static void getMethodsRecursive(Class<?> clazz, List<Method> result) {
        // search superclasses
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            result.addAll(Arrays.asList(Class.getDeclaredMethods(c, true)));
        }

        // search implemented interfaces
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Class<?> ifc : c.getInterfaces()) {
                getMethodsRecursive(ifc, result);
            }
        }
    }

    public static Member getConstructorOrMethod(Class<?> clazz, String name, boolean recursive,
            boolean publicOnly, Class<?>[] parameterTypes) throws NoSuchMethodException {
        if (recursive && !publicOnly) {
            throw new AssertionError(); // can't lookup non-public members recursively
        }
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (parameterTypes == null) {
            parameterTypes = EmptyArray.CLASS;
        }
        for (Class<?> c : parameterTypes) {
            if (c == null) {
                throw new NoSuchMethodException("parameter type is null");
            }
        }
        Member result = recursive
                ? getPublicConstructorOrMethodRecursive(clazz, name, parameterTypes)
                : Class.getDeclaredConstructorOrMethod(clazz, name, parameterTypes);
        if (result == null || publicOnly && (result.getModifiers() & Modifier.PUBLIC) == 0) {
            throw new NoSuchMethodException(name + " " + Arrays.toString(parameterTypes));
        }
        return result;
    }

    private static Member getPublicConstructorOrMethodRecursive(
            Class<?> clazz, String name, Class<?>[] parameterTypes) {
        // search superclasses
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            Member result = Class.getDeclaredConstructorOrMethod(c, name, parameterTypes);
            if (result != null && (result.getModifiers() & Modifier.PUBLIC) != 0) {
                return result;
            }
        }

        // search implemented interfaces
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Class<?> ifc : c.getInterfaces()) {
                Member result = getPublicConstructorOrMethodRecursive(ifc, name, parameterTypes);
                if (result != null && (result.getModifiers() & Modifier.PUBLIC) != 0) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Returns all public fields, both directly declared and inherited.
     */
    public static Field[] getAllPublicFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();
        Set<String> seen = new HashSet<String>();
        getFields(clazz, fields, seen);
        return fields.toArray(new Field[fields.size()]);
    }

    /**
     * Collects the list of fields without performing any security checks
     * first. This includes the fields inherited from superclasses and from
     * all implemented interfaces. The latter may also implement multiple
     * interfaces, so we (potentially) recursively walk through a whole tree of
     * classes. If no fields exist at all, an empty array is returned.
     *
     * @param clazz non-null; class to inspect
     * @param fields non-null; the target list to add the results to
     * @param seen non-null; a set of signatures we've already seen
     * or all of them
     */
    private static void getFields(Class<?> clazz, List<Field> fields, Set<String> seen) {
        // search superclasses
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Field field : Class.getDeclaredFields(clazz, true)) {
                String signature = field.toString();
                if (!seen.contains(signature)) {
                    fields.add(field);
                    seen.add(signature);
                }
            }
        }

        // search implemented interfaces
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Class<?> ifc : c.getInterfaces()) {
                getFields(ifc, fields, seen);
            }
        }
    }

    /**
     * Calls the static method <code>values()</code> on this
     * instance's class, which is presumed to be a properly-formed
     * enumeration class, using proper privilege hygiene.
     *
     * @return non-null; the array of values as reported by
     * <code>value()</code>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> T[] getEnumValuesInOrder(Class<T> clazz) {
        Method method = (Method) Class.getDeclaredConstructorOrMethod(
                clazz, "values", EmptyArray.CLASS);
        try {
            return (T[]) method.invoke((Object[]) null);
        } catch (IllegalAccessException impossible) {
            throw new AssertionError();
        } catch (InvocationTargetException impossible) {
            throw new AssertionError();
        }
    }
}
