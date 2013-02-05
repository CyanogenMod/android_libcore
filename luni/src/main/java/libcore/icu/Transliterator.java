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

/**
 * Exposes icu4c's Transliterator.
 */
public final class Transliterator {
  /**
   * Returns the ids of all known transliterators.
   */
  public static native String[] getAvailableIDs();

  /**
   * Transliterates 's' using the transliterator identified by 'id'.
   */
  public static native String transliterate(String id, String s);

  private Transliterator() {}
}
