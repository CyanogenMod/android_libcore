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

#ifndef JAVA_LANG_DOUBLE_H_included
#define JAVA_LANG_DOUBLE_H_included

#include <stdint.h>

union Double {
public:
    static inline double longBitsToDouble(uint64_t bits) {
        Double result;
        result.bits = bits;
        return result.doubleValue;
    }

    static inline uint64_t doubleToRawLongBits(double doubleValue) {
        Double result;
        result.doubleValue = doubleValue;
        return result.bits;
    }

private:
    uint64_t bits;
    double doubleValue;
};

#endif  // JAVA_LANG_DOUBLE_H_included
