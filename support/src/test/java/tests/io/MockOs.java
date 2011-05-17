/*
 * Copyright (C) 2011 The Android Open Source Project
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

package tests.io;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import libcore.io.ErrnoException;
import libcore.io.Libcore;
import libcore.io.Os;
import libcore.io.OsConstants;

/**
 * A mocking interceptor that wraps another {@link Os} to add faults. This can
 * be useful to test otherwise hard-to-test scenarios such as a full disk.
 */
public final class MockOs {
    private final InheritableThreadLocal<Map<String, List<Fault>>> faults
            = new InheritableThreadLocal<Map<String, List<Fault>>>() {
        @Override protected Map<String, List<Fault>> initialValue() {
            return new HashMap<String, List<Fault>>();
        }
    };

    private final InvocationHandler invocationHandler = new InvocationHandler() {
        @Override public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            Map<String, List<Fault>> threadFaults = faults.get();
            List<Fault> methodFaults = threadFaults.get(method.getName());
            if (methodFaults == null || methodFaults.isEmpty()) {
                try {
                    return method.invoke(delegate, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }

            Fault fault = methodFaults.remove(0);
            fault.trigger(method);
            return null;
        }
    };

    private final Os mockOs = (Os) Proxy.newProxyInstance(MockOs.class.getClassLoader(),
            new Class[] { Os.class }, invocationHandler);
    private Os delegate;

    public void install() {
        if (delegate != null) {
            throw new IllegalStateException("MockOs already installed!");
        }
        delegate = Libcore.os;
        Libcore.os = mockOs;
    }

    public void uninstall() {
        if (delegate == null) {
            throw new IllegalStateException("MockOs not installed!");
        }
        Libcore.os = delegate;
    }

    public void enqueueFault(String methodName) {
        enqueueFault(methodName, OsConstants.EIO);
    }

    public void enqueueFault(String methodName, int errno) {
        Map<String, List<Fault>> threadFaults = faults.get();
        List<Fault> methodFaults = threadFaults.get(methodName);
        if (methodFaults == null) {
            methodFaults = new ArrayList<Fault>();
            threadFaults.put(methodName, methodFaults);
        }
        methodFaults.add(new Fault(errno));
    }

    private static class Fault {
        private final int errno;
        public Fault(int errno) {
            this.errno = errno;
        }

        public void trigger(Method method) {
            throw new ErrnoException(method.getName(), errno);
        }
    }
}
