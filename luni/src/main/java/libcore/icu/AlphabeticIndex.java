/*
 * Copyright (C) 2013 The Android Open Source Project
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

package libcore.icu;

import java.util.Locale;

/**
 * Exposes icu4c's AlphabeticIndex.
 */
public final class AlphabeticIndex {
  private long peer;

  /**
   * Creates a new AlphabeticIndex for the given locale.
   */
  public AlphabeticIndex(Locale locale) {
    peer = create(locale.toString());
  }

  @Override protected synchronized void finalize() throws Throwable {
    try {
      destroy(peer);
      peer = 0;
    } finally {
      super.finalize();
    }
  }

  /**
   * Adds the index characters from the given locale to the index.
   * The labels are added to those that are already in the index;
   * they do not replace the existing index characters.
   * The collation order for this index is not changed;
   * it remains that of the locale that was originally specified
   * when creating this index.
   */
  public synchronized void addLabels(Locale locale) {
    addLabels(peer, locale.toString());
  }

  /**
   * Adds the index characters in the range between the specified start and
   * end code points, inclusive.
   */
  public synchronized void addLabelRange(int codePointStart, int codePointEnd) {
    addLabelRange(peer, codePointStart, codePointEnd);
  }

  /**
   * Returns the number of the label buckets in this index.
   */
  public synchronized int getBucketCount() {
    return getBucketCount(peer);
  }

  /**
   * Returns the index of the bucket in which 's' should appear.
   * Function is synchronized because underlying routine walks an iterator
   * whose state is maintained inside the index object.
   */
  public synchronized int getBucketIndex(String s) {
    return getBucketIndex(peer, s);
  }

  /**
   * Returns the label for the bucket at the given index (as returned by getBucketIndex).
   */
  public String getBucketLabel(int index) {
    return getBucketLabel(peer, index);
  }

  private static native long create(String locale);
  private static native void destroy(long peer);
  private static native void addLabels(long peer, String locale);
  private static native void addLabelRange(long peer, int codePointStart, int codePointEnd);
  private static native int getBucketCount(long peer);
  private static native int getBucketIndex(long peer, String s);
  private static native String getBucketLabel(long peer, int index);
}
