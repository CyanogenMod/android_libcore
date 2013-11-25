/*
 * Copyright (C) 2013 Google Inc.
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

package benchmarks;

import com.google.caliper.Param;
import com.google.caliper.SimpleBenchmark;

public class SystemArrayCopyBenchmark extends SimpleBenchmark {
  @Param({"2", "4", "8", "16", "32", "64", "128", "256", "512", "1024",
          "2048", "4096", "8192", "16384", "32768", "65536", "131072", "262144"})
  int arrayLength;

  // This copies a char array indirectly via String.getChars() as the
  // System.arraycopy() call site optimization currently works within
  // the core libraries only. Add direct System.arraycopy() benchmarks
  // (including ones for other primitive types) later once this
  // limitation goes away.
  public void timeStringCharArrayCopy(int reps) {
    final int len = arrayLength;
    char[] dst = new char[len];
    String str = new String(new char[len]);
    for (int rep = 0; rep < reps; ++rep) {
      str.getChars(0, len, dst, 0);
    }
  }
}
