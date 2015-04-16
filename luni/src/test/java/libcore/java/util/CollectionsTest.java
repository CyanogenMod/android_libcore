/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.java.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import junit.framework.TestCase;

public final class CollectionsTest extends TestCase {

    public void testEmptyEnumeration() {
        Enumeration<Object> e = Collections.emptyEnumeration();
        assertFalse(e instanceof Serializable);
        assertFalse(e.hasMoreElements());
        try {
            e.nextElement();
            fail();
        } catch (NoSuchElementException expected) {
        }
    }

    public void testEmptyIterator() {
        testEmptyIterator(Collections.emptyIterator());
        testEmptyIterator(Collections.emptyList().iterator());
        testEmptyIterator(Collections.emptySet().iterator());
        testEmptyIterator(Collections.emptyMap().keySet().iterator());
        testEmptyIterator(Collections.emptyMap().entrySet().iterator());
        testEmptyIterator(Collections.emptyMap().values().iterator());
    }

    private void testEmptyIterator(Iterator<?> i) {
        assertFalse(i instanceof Serializable);
        assertFalse(i.hasNext());
        try {
            i.next();
            fail();
        } catch (NoSuchElementException expected) {
        }
        try {
            i.remove();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    public void testEmptyListIterator() {
        testEmptyListIterator(Collections.emptyListIterator());
        testEmptyListIterator(Collections.emptyList().listIterator());
        testEmptyListIterator(Collections.emptyList().listIterator(0));
    }

    private void testEmptyListIterator(ListIterator<?> i) {
        assertFalse(i instanceof Serializable);
        assertFalse(i.hasNext());
        assertFalse(i.hasPrevious());
        assertEquals(0, i.nextIndex());
        try {
            i.next();
            fail();
        } catch (NoSuchElementException expected) {
        }
        assertEquals(-1, i.previousIndex());
        try {
            i.previous();
            fail();
        } catch (NoSuchElementException expected) {
        }
        try {
            i.add(null);
            fail();
        } catch (UnsupportedOperationException expected) {
        }
        try {
            i.remove();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    public void testSortFastPath_incrementsModcount() {
        ArrayList<String> list = new ArrayList<String>(16);
        list.add("coven");
        list.add("asylum");
        list.add("murder house");
        list.add("freak show");

        Iterator<String> it = list.iterator();
        it.next();
        Collections.sort(list);
        try {
            it.next();
            fail();
        } catch (ConcurrentModificationException expected) {
        }
    }

    /**
     * A value type whose {@code compareTo} method returns one of {@code 0},
     * {@code Integer.MIN_VALUE} and {@code Integer.MAX_VALUE}.
     */
    static final class IntegerWithExtremeComparator
            implements Comparable<IntegerWithExtremeComparator> {
        private final int value;

        public IntegerWithExtremeComparator(int value) {
            this.value = value;
        }

        @Override
        public int compareTo(IntegerWithExtremeComparator another) {
            if (another.value == this.value) {
                return 0;
            } else if (another.value > this.value) {
                return Integer.MIN_VALUE;
            } else {
                return Integer.MAX_VALUE;
            }
        }
    }

    // http://b/19749094
    public void testBinarySearch_comparatorThatReturnsMinAndMaxValue() {
        ArrayList<Integer> list = new ArrayList<Integer>(16);
        list.add(4);
        list.add(9);
        list.add(11);
        list.add(14);
        list.add(16);

        int index = Collections.binarySearch(list, 9, new Comparator<Integer>() {
            @Override
            public int compare(Integer lhs, Integer rhs) {
                final int compare = lhs.compareTo(rhs);
                if (compare == 0) {
                    return 0;
                } else if (compare < 0) {
                    return Integer.MIN_VALUE;
                } else {
                    return Integer.MAX_VALUE;
                }
            }
        });
        assertEquals(1, index);

        ArrayList<IntegerWithExtremeComparator> list2 =
                new ArrayList<IntegerWithExtremeComparator>();
        list2.add(new IntegerWithExtremeComparator(4));
        list2.add(new IntegerWithExtremeComparator(9));
        list2.add(new IntegerWithExtremeComparator(11));
        list2.add(new IntegerWithExtremeComparator(14));
        list2.add(new IntegerWithExtremeComparator(16));

        assertEquals(1, Collections.binarySearch(list2, new IntegerWithExtremeComparator(9)));
    }

    public void testBinarySearch_emptyCollection() {
        assertEquals(-1, Collections.binarySearch(new ArrayList<Integer>(), 9));

        assertEquals(-1, Collections.binarySearch(new ArrayList<Integer>(), 9,
                new Comparator<Integer>() {
                    @Override
                    public int compare(Integer lhs, Integer rhs) {
                        return lhs.compareTo(rhs);
                    }
                }));
    }
}
