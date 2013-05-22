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
/*
 * Copyright (C) 2012 The Android Open Source Project
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

package java.lang.reflect;

import com.android.dex.Dex;
import com.android.dex.ProtoId;
import com.android.dex.TypeList;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import libcore.reflect.AnnotationAccess;
import libcore.reflect.GenericSignatureParser;
import libcore.reflect.InternalNames;
import libcore.reflect.ListOfTypes;
import libcore.reflect.Types;

/**
 * This class represents an abstract method. Abstract methods are either methods or constructors.
 * @hide
 */
public abstract class AbstractMethod extends AccessibleObject {
    private static final Comparator<AbstractMethod> ORDER_BY_SIGNATURE = null;

    /** Method's declaring class */
    Class<?> declaringClass;
    /** Method access flags (modifiers) */
    private int accessFlags;
    /** DexFile index */
    int methodDexIndex;
    /** Dispatch table entry */
    private int methodIndex;
    /** DexFile offset of CodeItem for this Method */
    private int codeItemOffset;
    /* ART compiler meta-data */
    private int frameSizeInBytes;
    private int coreSpillMask;
    private int fpSpillMask;
    private int mappingTable;
    private int gcMap;
    private int vmapTable;
    /** ART: compiled managed code associated with this Method */
    private int entryPointFromCompiledCode;
    /** ART: entry point from interpreter associated with this Method */
    private int entryPointFromInterpreter;
    /** ART: if this is a native method, the native code that will be invoked */
    private int nativeMethod;
    /* ART: dex cache fast access */
    private String[] dexCacheStrings;
    Class<?>[] dexCacheResolvedTypes;
    AbstractMethod[] dexCacheResolvedMethods;
    private Object[] dexCacheInitializedStaticStorage;

    /**
     * Only created by native code.
     */
    AbstractMethod() {
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return super.getAnnotation(annotationClass);
    }

    /**
     * We insert native method stubs for abstract methods so we don't have to
     * check the access flags at the time of the method call.  This results in
     * "native abstract" methods, which can't exist.  If we see the "abstract"
     * flag set, clear the "native" flag.
     *
     * We also move the DECLARED_SYNCHRONIZED flag into the SYNCHRONIZED
     * position, because the callers of this function are trying to convey
     * the "traditional" meaning of the flags to their callers.
     */
    private static int fixMethodFlags(int flags) {
        if ((flags & Modifier.ABSTRACT) != 0) {
            flags &= ~Modifier.NATIVE;
        }
        flags &= ~Modifier.SYNCHRONIZED;
        int ACC_DECLARED_SYNCHRONIZED = 0x00020000;
        if ((flags & ACC_DECLARED_SYNCHRONIZED) != 0) {
            flags |= Modifier.SYNCHRONIZED;
        }
        return flags & 0xffff;  // mask out bits not used by Java
    }

    int getModifiers() {
        return fixMethodFlags(accessFlags);
    }

    boolean isVarArgs() {
        return (accessFlags & Modifier.VARARGS) != 0;
    }

    boolean isBridge() {
        return (accessFlags & Modifier.BRIDGE) != 0;
    }

    public boolean isSynthetic() {
        return (accessFlags & Modifier.SYNTHETIC) != 0;
    }

    /**
     * @hide
     */
    public final int getAccessFlags() {
        return accessFlags;
    }

    /**
     * Returns the name of the method or constructor represented by this
     * instance.
     *
     * @return the name of this method
     */
    public abstract String getName();

    /**
     * Returns the class that declares this constructor or method.
     */
    Class<?> getDeclaringClass() {
        return declaringClass;
    }

    public int getDexMethodIndex() {
        return methodDexIndex;
    }

    /**
     * Returns the exception types as an array of {@code Class} instances. If
     * this method has no declared exceptions, an empty array is returned.
     *
     * @return the declared exception classes
     */
    protected Class<?>[] getExceptionTypes() {
        if (declaringClass.isProxy()) {
            return getExceptionTypesNative();
        } else {
            // TODO: use dex cache to speed looking up class
            return AnnotationAccess.getExceptions(this);
        }
    }

    private native Class<?>[] getExceptionTypesNative();

    /**
     * Returns an array of {@code Class} objects associated with the parameter types of this
     * abstract method. If the method was declared with no parameters, an
     * empty array will be returned.
     *
     * @return the parameter types
     */
    public abstract Class<?>[] getParameterTypes();

    /**
     * Returns true if {@code other} has the same declaring class, name,
     * parameters and return type as this method.
     */
    @Override public boolean equals(Object other) {
        return this == other; // exactly one instance of each member in this runtime
    }

    String toGenericString() {
        return toGenericStringHelper();
    }

    Type[] getGenericParameterTypes() {
        return Types.getClonedTypeArray(getMethodOrConstructorGenericInfo().genericParameterTypes);
    }

    Type[] getGenericExceptionTypes() {
        return Types.getClonedTypeArray(getMethodOrConstructorGenericInfo().genericExceptionTypes);
    }

    @Override public Annotation[] getDeclaredAnnotations() {
        List<Annotation> result = AnnotationAccess.getDeclaredAnnotations(this);
        return result.toArray(new Annotation[result.size()]);
    }

    @Override public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            throw new NullPointerException("annotationType == null");
        }
        return AnnotationAccess.isDeclaredAnnotationPresent(this, annotationType);
    }

    public Annotation[] getAnnotations() {
        return super.getAnnotations();
    }

    /**
     * Returns an array of arrays that represent the annotations of the formal
     * parameters of this method. If there are no parameters on this method,
     * then an empty array is returned. If there are no annotations set, then
     * and array of empty arrays is returned.
     *
     * @return an array of arrays of {@code Annotation} instances
     */
    public abstract Annotation[][] getParameterAnnotations();

    /**
     * Returns the constructor's signature in non-printable form. This is called
     * (only) from IO native code and needed for deriving the serialVersionUID
     * of the class
     *
     * @return The constructor's signature.
     */
    @SuppressWarnings("unused")
    abstract String getSignature();

    /**
     * Returns a string from the dex cache, computing the string from the dex file if necessary.
     * Note this method replicates {@link java.lang.Class#getDexCacheString(Dex, int)}, but in
     * Method we can avoid one indirection.
     */
    String getDexCacheString(Dex dex, int dexStringIndex) {
        String s = (String) dexCacheStrings[dexStringIndex];
        if (s == null) {
            s = dex.strings().get(dexStringIndex);
            dexCacheStrings[dexStringIndex] = s;
        }
        return s;
    }

    /**
     * Returns a resolved type from the dex cache, computing the string from the dex file if
     * necessary. Note this method replicates {@link java.lang.Class#getDexCacheType(Dex, int)},
     * but in Method we can avoid one indirection.
     */
    Class<?> getDexCacheType(Dex dex, int dexTypeIndex) {
        Class<?> resolvedType = dexCacheResolvedTypes[dexTypeIndex];
        if (resolvedType == null) {
            int descriptorIndex = dex.typeIds().get(dexTypeIndex);
            String descriptor = getDexCacheString(dex, descriptorIndex);
            resolvedType = InternalNames.getClass(declaringClass.getClassLoader(), descriptor);
            dexCacheResolvedTypes[dexTypeIndex] = resolvedType;
        }
        return resolvedType;
    }

    static final class GenericInfo {
        final ListOfTypes genericExceptionTypes;
        final ListOfTypes genericParameterTypes;
        final Type genericReturnType;
        final TypeVariable<?>[] formalTypeParameters;

        GenericInfo(ListOfTypes exceptions, ListOfTypes parameters, Type ret,
                    TypeVariable<?>[] formal) {
            genericExceptionTypes = exceptions;
            genericParameterTypes = parameters;
            genericReturnType = ret;
            formalTypeParameters = formal;
        }
    }

    /**
     * Returns generic information associated with this method/constructor member.
     */
    GenericInfo getMethodOrConstructorGenericInfo() {
        String signatureAttribute = AnnotationAccess.getSignature(this);
        Member member;
        Class<?>[] exceptionTypes;
        boolean method = this instanceof Method;
        if (method) {
            Method m = (Method) this;
            member = m;
            exceptionTypes = m.getExceptionTypes();
        } else {
            Constructor<?> c = (Constructor<?>) this;
            member = c;
            exceptionTypes = c.getExceptionTypes();
        }
        GenericSignatureParser parser =
            new GenericSignatureParser(member.getDeclaringClass().getClassLoader());
        if (method) {
            parser.parseForMethod((GenericDeclaration) this, signatureAttribute, exceptionTypes);
        } else {
            parser.parseForConstructor((GenericDeclaration) this, signatureAttribute, exceptionTypes);
        }
        return new GenericInfo(parser.exceptionTypes, parser.parameterTypes,
                               parser.returnType, parser.formalTypeParameters);
    }

    /**
     * Helper for Method and Constructor for toGenericString
     */
    String toGenericStringHelper() {
        StringBuilder sb = new StringBuilder(80);
        GenericInfo info =  getMethodOrConstructorGenericInfo();
        int modifiers = ((Member)this).getModifiers();
        // append modifiers if any
        if (modifiers != 0) {
            sb.append(Modifier.toString(modifiers & ~Modifier.VARARGS)).append(' ');
        }
        // append type parameters
        if (info.formalTypeParameters != null && info.formalTypeParameters.length > 0) {
            sb.append('<');
            for (int i = 0; i < info.formalTypeParameters.length; i++) {
                Types.appendGenericType(sb, info.formalTypeParameters[i]);
                if (i < info.formalTypeParameters.length - 1) {
                    sb.append(",");
                }
            }
            sb.append("> ");
        }
        Class<?> declaringClass = ((Member) this).getDeclaringClass();
        if (this instanceof Constructor) {
            // append constructor name
            Types.appendTypeName(sb, declaringClass);
        } else {
            // append return type
            Types.appendGenericType(sb, Types.getType(info.genericReturnType));
            sb.append(' ');
            // append method name
            Types.appendTypeName(sb, declaringClass);
            sb.append(".").append(((Method) this).getName());
        }
        // append parameters
        sb.append('(');
        Types.appendArrayGenericType(sb, info.genericParameterTypes.getResolvedTypes());
        sb.append(')');
        // append exceptions if any
        Type[] genericExceptionTypeArray =
                Types.getClonedTypeArray(info.genericExceptionTypes);
        if (genericExceptionTypeArray.length > 0) {
            sb.append(" throws ");
            Types.appendArrayGenericType(sb, genericExceptionTypeArray);
        }
        return sb.toString();
    }

}
