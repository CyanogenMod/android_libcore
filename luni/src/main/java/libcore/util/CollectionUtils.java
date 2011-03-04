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

package libcore.util;

import java.lang.ref.Reference;
import java.util.Iterator;

public final class CollectionUtils {
    private CollectionUtils() {}

    /**
     * Returns an iterator over the values referenced by the elements of {@code
     * iterable}.
     *
     * @param trim true to remove reference objects from the iterable after
     *     their referenced values have been cleared.
     */
    public static <T> Iterable<T> dereferenceIterable(
            final Iterable<? extends Reference<T>> iterable, final boolean trim) {
        return new Iterable<T>() {
            public Iterator<T> iterator() {
                return new Iterator<T>() {
                    private final Iterator<? extends Reference<T>> delegate = iterable.iterator();
                    private boolean removeIsOkay;
                    private T next;

                    private void computeNext() {
                        removeIsOkay = false;
                        while (next == null && delegate.hasNext()) {
                            next = delegate.next().get();
                            if (trim && next == null) {
                                delegate.remove();
                            }
                        }
                    }

                    @Override public boolean hasNext() {
                        computeNext();
                        return next != null;
                    }

                    @Override public T next() {
                        if (!hasNext()) {
                            throw new IllegalStateException();
                        }
                        T result = next;
                        removeIsOkay = true;
                        next = null;
                        return result;
                    }

                    public void remove() {
                        if (!removeIsOkay) {
                            throw new IllegalStateException();
                        }
                        delegate.remove();
                    }
                };
            }
        };
    }
}
