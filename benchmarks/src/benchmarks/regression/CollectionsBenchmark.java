/*
 * Copyright (C) 2014 The Android Open Source Project
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

package benchmarks.regression;

import com.google.caliper.Param;
import com.google.caliper.SimpleBenchmark;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

public class CollectionsBenchmark extends SimpleBenchmark {
    @Param({"4", "16", "64", "256", "1024"})
    private int arrayListLength;

    public void timeSort_arrayList(int nreps) {
        List<Integer> input = buildList(arrayListLength, true /* use array list */);
        for (int i = 0; i < nreps; ++i) {
            Collections.sort(input);
        }
    }

    public void timeSort_vector(int nreps) {
        List<Integer> input = buildList(arrayListLength, false /* use array list */);
        for (int i = 0; i < nreps; ++i) {
            Collections.sort(input);
        }
    }

    private static List<Integer> buildList(int arrayListLength, boolean useArrayList) {
        Random random = new Random();
        random.setSeed(0);
        List<Integer> list = useArrayList ? new ArrayList<Integer>(arrayListLength) :
                new Vector<Integer>(arrayListLength);
        for (int i = 0; i < arrayListLength; ++i) {
            list.add(random.nextInt());
        }
        return list;
    }
}
