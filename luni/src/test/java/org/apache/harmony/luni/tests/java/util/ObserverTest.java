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

package org.apache.harmony.luni.tests.java.util;

import junit.framework.TestCase;

import java.util.Observable;
import java.util.Observer;

public class ObserverTest extends TestCase {

    class Mock_Observer implements Observer {
        int updateCount = 0;

        public void update(Observable observed, Object arg) {
            ++updateCount;
        }

        public int getUpdateCount() {
            return updateCount;
        }
    }

    class TestObservable extends Observable {
        public void doChange() {
            setChanged();
        }

        public void clearChange() {
            clearChanged();
        }
    }

    public void testUpdate() {
        TestObservable observable = new TestObservable();
        Mock_Observer observer = null;
        observable.addObserver(observer = new Mock_Observer());
        observable.notifyObservers();
        assertEquals("Notified when unchnaged", 0, observer.getUpdateCount());
        observable.doChange();
        observable.notifyObservers();
        assertEquals("Failed to notify", 1, observer.getUpdateCount());
    }
}
