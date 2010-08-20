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

#ifndef JAVA_LANG_FLOAT_H_included
#define JAVA_LANG_FLOAT_H_included

#include <stdint.h>

union Float {
public:
    static inline float intBitsToFloat(uint32_t bits) {
        Float result;
        result.bits = bits;
        return result.floatValue;
    }

    static inline uint32_t floatToRawIntBits(float floatValue) {
        Float result;
        result.floatValue = floatValue;
        return result.bits;
    }

private:
    uint32_t bits;
    float floatValue;
};

#endif  // JAVA_LANG_FLOAT_H_included
