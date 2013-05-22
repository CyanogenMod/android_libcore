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

package java.lang.reflect;

import com.android.dex.Dex;
import com.android.dex.ProtoId;
import com.android.dex.TypeList;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import libcore.reflect.AnnotationAccess;
import libcore.reflect.InternalNames;
import libcore.reflect.Types;

/**
 * This class represents a method. Information about the method can be accessed,
 * and the method can be invoked dynamically.
 */
public final class Method extends AbstractMethod implements GenericDeclaration, Member {

    /**
     * Orders methods by their name, parameters and return type.
     *
     * @hide
     */
    public static final Comparator<Method> ORDER_BY_SIGNATURE = new Comparator<Method>() {
        @Override public int compare(Method a, Method b) {
            if (a == b) {
                return 0;
            }
            int comparison = a.getName().compareTo(b.getName());
            if (comparison != 0) {
                return comparison;
            }
            Class<?>[] aParameters = a.getParameterTypes();
            Class<?>[] bParameters = b.getParameterTypes();
            int length = Math.min(aParameters.length, bParameters.length);
            for (int i = 0; i < length; i++) {
                comparison = aParameters[i].getName().compareTo(bParameters[i].getName());
                if (comparison != 0) {
                    return comparison;
                }
            }
            if (aParameters.length != bParameters.length) {
                return aParameters.length - bParameters.length;
            }
            // this is necessary for methods that have covariant return types.
            return a.getReturnType().getName().compareTo(b.getReturnType().getName());
        }
    };

    /**
     * Only created by native code.
     */
    private Method() {
    }

    public Annotation[] getAnnotations() {
        return super.getAnnotations();
    }

    /**
     * Returns the modifiers for this method. The {@link Modifier} class should
     * be used to decode the result.
     *
     * @return the modifiers for this method
     *
     * @see Modifier
     */
    @Override public int getModifiers() {
        return super.getModifiers();
    }

    /**
     * Indicates whether or not this method takes a variable number argument.
     *
     * @return {@code true} if a vararg is declared, {@code false} otherwise
     */
    public boolean isVarArgs() {
        return super.isVarArgs();
    }

    /**
     * Indicates whether or not this method is a bridge.
     *
     * @return {@code true} if this method is a bridge, {@code false} otherwise
     */
    public boolean isBridge() {
        return super.isBridge();

    }

    /**
     * Indicates whether or not this method is synthetic.
     *
     * @return {@code true} if this method is synthetic, {@code false} otherwise
     */
    @Override public boolean isSynthetic() {
        return super.isSynthetic();
    }

    /**
     * Returns the name of the method represented by this {@code Method}
     * instance.
     *
     * @return the name of this method
     */
    @Override public String getName() {
        Method method = this;
        if (declaringClass.isProxy()) {
            // For proxies use their interface method
            method = findOverriddenMethod();
        }
        Dex dex = method.declaringClass.getDex();
        int nameIndex = dex.methodIds().get(methodDexIndex).getNameIndex();
        // Note, in the case of a Proxy the dex cache strings are equal.
        return getDexCacheString(dex, nameIndex);
    }

    /**
     * Returns the class that declares this method.
     */
    @Override public Class<?> getDeclaringClass() {
        return super.getDeclaringClass();
    }

    /**
     * Returns the index of this method's ID in its dex file.
     *
     * @hide
     */
    public int getDexMethodIndex() {
        return super.getDexMethodIndex();
    }

    /**
     * Returns the exception types as an array of {@code Class} instances. If
     * this method has no declared exceptions, an empty array is returned.
     *
     * @return the declared exception classes
     */
    public Class<?>[] getExceptionTypes() {
        if (declaringClass.isProxy()) {
            return getExceptionTypesNative();
        } else {
            // TODO: use dex cache to speed looking up class
            return AnnotationAccess.getExceptions(this);
        }
    }

    private native Class<?>[] getExceptionTypesNative();

    /**
     * Returns an array of {@code Class} objects associated with the parameter
     * types of this method. If the method was declared with no parameters, an
     * empty array will be returned.
     *
     * @return the parameter types
     */
    public Class<?>[] getParameterTypes() {
        Method method = this;
        if (declaringClass.isProxy()) {
            // For proxies use their interface method
            method = findOverriddenMethod();
        }
        Dex dex = method.declaringClass.getDex();
        int protoIndex = dex.methodIds().get(methodDexIndex).getProtoIndex();
        ProtoId proto = dex.protoIds().get(protoIndex);
        TypeList parametersList = dex.readTypeList(proto.getParametersOffset());
        short[] types = parametersList.getTypes();
        Class<?>[] parametersArray = new Class[types.length];
        for (int i = 0; i < types.length; i++) {
            // Note, in the case of a Proxy the dex cache types are equal.
            parametersArray[i] = getDexCacheType(dex, types[i]);
        }
        return parametersArray;
    }

    /**
     * Returns the {@code Class} associated with the return type of this
     * method.
     *
     * @return the return type
     */
    public Class<?> getReturnType() {
        Method method = this;
        if (declaringClass.isProxy()) {
            // For proxies use their interface method
            method = findOverriddenMethod();
        }
        Dex dex = method.declaringClass.getDex();
        int proto_idx = dex.methodIds().get(methodDexIndex).getProtoIndex();
        ProtoId proto = dex.protoIds().get(proto_idx);
        int returnTypeIndex = proto.getReturnTypeIndex();
        // Note, in the case of a Proxy the dex cache types are equal.
        return getDexCacheType(dex, returnTypeIndex);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Equivalent to {@code getDeclaringClass().getName().hashCode() ^ getName().hashCode()}.
     */
    @Override public int hashCode() {
        return getDeclaringClass().getName().hashCode() ^ getName().hashCode();
    }

    /**
     * Returns true if {@code other} has the same declaring class, name,
     * parameters and return type as this method.
     */
    @Override public boolean equals(Object other) {
        return this == other; // exactly one instance of each member in this runtime
    }

    /**
     * Returns true if this and {@code method} have the same name and the same
     * parameters in the same order. Such methods can share implementation if
     * one method's return types is assignable to the other.
     *
     * @hide needed by Proxy
     */
    boolean equalNameAndParameters(Method m) {
        if (!getName().equals(m.getName())) {
            return false;
        }
        if (!Arrays.equals(getParameterTypes(), m.getParameterTypes())) {
            return false;
        }
        return true;
    }

    /**
     * Returns the string representation of the method's declaration, including
     * the type parameters.
     *
     * @return the string representation of this method
     */
    public String toGenericString() {
        return super.toGenericString();
    }

    @Override public TypeVariable<Method>[] getTypeParameters() {
        GenericInfo info = getMethodOrConstructorGenericInfo();
        return (TypeVariable<Method>[]) info.formalTypeParameters.clone();
    }

    /**
     * Returns the parameter types as an array of {@code Type} instances, in
     * declaration order. If this method has no parameters, an empty array is
     * returned.
     *
     * @return the parameter types
     *
     * @throws GenericSignatureFormatError
     *             if the generic method signature is invalid
     * @throws TypeNotPresentException
     *             if any parameter type points to a missing type
     * @throws MalformedParameterizedTypeException
     *             if any parameter type points to a type that cannot be
     *             instantiated for some reason
     */
    public Type[] getGenericParameterTypes() {
        return Types.getClonedTypeArray(getMethodOrConstructorGenericInfo().genericParameterTypes);
    }

    @Override public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
        if (annotationType == null) {
            throw new NullPointerException("annotationType == null");
        }
        return AnnotationAccess.isDeclaredAnnotationPresent(this, annotationType);
    }

    /**
     * Returns the exception types as an array of {@code Type} instances. If
     * this method has no declared exceptions, an empty array will be returned.
     *
     * @return an array of generic exception types
     *
     * @throws GenericSignatureFormatError
     *             if the generic method signature is invalid
     * @throws TypeNotPresentException
     *             if any exception type points to a missing type
     * @throws MalformedParameterizedTypeException
     *             if any exception type points to a type that cannot be
     *             instantiated for some reason
     */
    public Type[] getGenericExceptionTypes() {
        return Types.getClonedTypeArray(getMethodOrConstructorGenericInfo().genericExceptionTypes);
    }

    /**
     * Returns the return type of this method as a {@code Type} instance.
     *
     * @return the return type of this method
     *
     * @throws GenericSignatureFormatError
     *             if the generic method signature is invalid
     * @throws TypeNotPresentException
     *             if the return type points to a missing type
     * @throws MalformedParameterizedTypeException
     *             if the return type points to a type that cannot be
     *             instantiated for some reason
     */
    public Type getGenericReturnType() {
        return Types.getType(getMethodOrConstructorGenericInfo().genericReturnType);
    }

    @Override public Annotation[] getDeclaredAnnotations() {
        List<Annotation> result = AnnotationAccess.getDeclaredAnnotations(this);
        return result.toArray(new Annotation[result.size()]);
    }

    @Override public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
        if (annotationType == null) {
            throw new NullPointerException("annotationType == null");
        }
        return AnnotationAccess.getDeclaredAnnotation(this, annotationType);
    }

    /**
     * Returns an array of arrays that represent the annotations of the formal
     * parameters of this method. If there are no parameters on this method,
     * then an empty array is returned. If there are no annotations set, then
     * and array of empty arrays is returned.
     *
     * @return an array of arrays of {@code Annotation} instances
     */
    public Annotation[][] getParameterAnnotations() {
        Method method = this;
        if (declaringClass.isProxy()) {
            // For proxies use their interface method
            method = findOverriddenMethod();
        }
        return AnnotationAccess.getParameterAnnotations(method);
    }

    /**
     * Returns the default value for the annotation member represented by this
     * method.
     *
     * @return the default value, or {@code null} if none
     *
     * @throws TypeNotPresentException
     *             if this annotation member is of type {@code Class} and no
     *             definition can be found
     */
    public Object getDefaultValue() {
        return AnnotationAccess.getDefaultValue(this);
    }

    /**
     * Returns the result of dynamically invoking this method. Equivalent to
     * {@code receiver.methodName(arg1, arg2, ... , argN)}.
     *
     * <p>If the method is static, the receiver argument is ignored (and may be null).
     *
     * <p>If the method takes no arguments, you can pass {@code (Object[]) null} instead of
     * allocating an empty array.
     *
     * <p>If you're calling a varargs method, you need to pass an {@code Object[]} for the
     * varargs parameter: that conversion is usually done in {@code javac}, not the VM, and
     * the reflection machinery does not do this for you. (It couldn't, because it would be
     * ambiguous.)
     *
     * <p>Reflective method invocation follows the usual process for method lookup.
     *
     * <p>If an exception is thrown during the invocation it is caught and
     * wrapped in an InvocationTargetException. This exception is then thrown.
     *
     * <p>If the invocation completes normally, the return value itself is
     * returned. If the method is declared to return a primitive type, the
     * return value is boxed. If the return type is void, null is returned.
     *
     * @param receiver
     *            the object on which to call this method (or null for static methods)
     * @param args
     *            the arguments to the method
     * @return the result
     *
     * @throws NullPointerException
     *             if {@code receiver == null} for a non-static method
     * @throws IllegalAccessException
     *             if this method is not accessible (see {@link AccessibleObject})
     * @throws IllegalArgumentException
     *             if the number of arguments doesn't match the number of parameters, the receiver
     *             is incompatible with the declaring class, or an argument could not be unboxed
     *             or converted by a widening conversion to the corresponding parameter type
     * @throws InvocationTargetException
     *             if an exception was thrown by the invoked method
     */
    public native Object invoke(Object receiver, Object... args)
        throws IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    /**
     * Returns a string containing a concise, human-readable description of this
     * method. The format of the string is:
     *
     * <ol>
     *   <li>modifiers (if any)
     *   <li>return type or 'void'
     *   <li>declaring class name
     *   <li>'('
     *   <li>parameter types, separated by ',' (if any)
     *   <li>')'
     *   <li>'throws' plus exception types, separated by ',' (if any)
     * </ol>
     *
     * For example: {@code public native Object
     * java.lang.Method.invoke(Object,Object) throws
     * IllegalAccessException,IllegalArgumentException
     * ,InvocationTargetException}
     *
     * @return a printable representation for this method
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(Modifier.toString(getModifiers()));

        if (result.length() != 0) {
            result.append(' ');
        }
        result.append(getReturnType().getName());
        result.append(' ');
        result.append(getDeclaringClass().getName());
        result.append('.');
        result.append(getName());
        result.append("(");
        Class<?>[] parameterTypes = getParameterTypes();
        result.append(Types.toString(parameterTypes));
        result.append(")");
        Class<?>[] exceptionTypes = getExceptionTypes();
        if (exceptionTypes.length != 0) {
            result.append(" throws ");
            result.append(Types.toString(exceptionTypes));
        }
        return result.toString();
    }

    /**
     * Returns the constructor's signature in non-printable form. This is called
     * (only) from IO native code and needed for deriving the serialVersionUID
     * of the class
     *
     * @return The constructor's signature.
     */
    @SuppressWarnings("unused")
    String getSignature() {
        StringBuilder result = new StringBuilder();

        result.append('(');
        Class<?>[] parameterTypes = getParameterTypes();
        for (Class<?> parameterType : parameterTypes) {
            result.append(Types.getSignature(parameterType));
        }
        result.append(')');
        result.append(Types.getSignature(getReturnType()));

        return result.toString();
    }

    public void setAccessible(boolean flag) {
        super.setAccessible(flag);
    }

    /**
     * Returns a string from the dex cache, computing the string from the dex file if necessary.
     * Note this method replicates {@link java.lang.Class#getDexCacheString(Dex, int)}, but in
     * Method we can avoid one indirection.
     */
    String getDexCacheString(Dex dex, int dexStringIndex) {
        return super.getDexCacheString(dex, dexStringIndex);
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

    /**
     * Returns the {@code Method} that this method overrides. Used to determine the interface
     * method overridden by a proxy method (as the proxy method doesn't directly support operations
     * such as {@link Method#getName}). This method works for non-proxy methods.
     */
    private Method findOverriddenMethod() {
      if (declaringClass.isProxy()) {
        // Proxy method's declaring class' dex cache refers to that of Proxy. The local cache in
        // Method refers to the original interface's dex cache and is ensured to be resolved by
        // proxy generation. Short-cut the native call below in this case.
        return (Method) dexCacheResolvedMethods[methodDexIndex];
      } else {
        return findOverriddenMethodNative();
      }
    }

    private native Method findOverriddenMethodNative();
}
