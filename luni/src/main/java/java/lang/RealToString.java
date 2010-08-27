/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package java.lang;

import libcore.math.MathUtils;

final class RealToString {
    private int setCount; // number of times u and k have been gotten

    private int getCount; // number of times u and k have been set

    private int[] uArray = new int[64];

    private int firstK;

    private final static double invLogOfTenBaseTwo = Math.log(2.0) / Math.log(10.0);

    public String doubleToString(double inputNumber) {
        long inputNumberBits = Double.doubleToLongBits(inputNumber);
        boolean positive = (inputNumberBits & Double.SIGN_MASK) == 0;
        int e = (int) ((inputNumberBits & Double.EXPONENT_MASK) >> Double.MANTISSA_BITS);
        long f = inputNumberBits & Double.MANTISSA_MASK;
        boolean mantissaIsZero = f == 0;

        if (e == 2047) {
            if (!mantissaIsZero) {
                return "NaN";
            }
            return positive ? "Infinity" : "-Infinity";
        }
        int p = Double.EXPONENT_BIAS + Double.MANTISSA_BITS; // the power offset (precision)
        int pow = 0, numBits = Double.MANTISSA_BITS;
        if (e == 0) {
            if (mantissaIsZero) {
                return positive ? "0.0" : "-0.0";
            }
            if (f == 1) {
                // special case to increase precision even though 2 * Double.MIN_VALUE is 1.0e-323
                return positive ? "4.9E-324" : "-4.9E-324";
            }
            pow = 1 - p; // a denormalized number
            long ff = f;
            while ((ff & 0x0010000000000000L) == 0) {
                ff = ff << 1;
                numBits--;
            }
        } else {
            // 0 < e < 2047
            // a "normalized" number
            f = f | 0x0010000000000000L;
            pow = e - p;
        }

        if (-59 < pow && pow < 6 || (pow == -59 && !mantissaIsZero)) {
            longDigitGenerator(f, pow, e == 0, mantissaIsZero, numBits);
        } else {
            bigIntDigitGenerator(f, pow, e == 0, numBits);
        }
        if (inputNumber >= 1e7D || inputNumber <= -1e7D
                || (inputNumber > -1e-3D && inputNumber < 1e-3D)) {
            return freeFormatExponential(positive);
        }
        return freeFormat(positive);
    }

    public String floatToString(float inputNumber) {
        int inputNumberBits = Float.floatToIntBits(inputNumber);
        boolean positive = (inputNumberBits & Float.SIGN_MASK) == 0;
        // the value of the 'power bits' of the inputNumber
        int e = (inputNumberBits & Float.EXPONENT_MASK) >> Float.MANTISSA_BITS;
        // the value of the 'significand bits' of the inputNumber
        int f = inputNumberBits & Float.MANTISSA_MASK;
        boolean mantissaIsZero = f == 0;

        if (e == 255) {
            if (!mantissaIsZero) {
                return "NaN";
            }
            return positive ? "Infinity" : "-Infinity";
        }
        int p = Float.EXPONENT_BIAS + Float.MANTISSA_BITS; // the power offset (precision)
        int pow = 0, numBits = Float.MANTISSA_BITS;
        if (e == 0) {
            if (mantissaIsZero) {
                return positive ? "0.0" : "-0.0";
            }
            pow = 1 - p; // a denormalized number
            if (f < 8) { // want more precision with smallest values
                f = f << 2;
                pow -= 2;
            }
            int ff = f;
            while ((ff & 0x00800000) == 0) {
                ff = ff << 1;
                numBits--;
            }
        } else {
            // 0 < e < 255
            // a "normalized" number
            f = f | 0x00800000;
            pow = e - p;
        }

        if (-59 < pow && pow < 35 || (pow == -59 && !mantissaIsZero)) {
            longDigitGenerator(f, pow, e == 0, mantissaIsZero, numBits);
        } else {
            bigIntDigitGenerator(f, pow, e == 0, numBits);
        }
        if (inputNumber >= 1e7f || inputNumber <= -1e7f
                || (inputNumber > -1e-3f && inputNumber < 1e-3f)) {
            return freeFormatExponential(positive);
        }
        return freeFormat(positive);
    }

    private String freeFormatExponential(boolean positive) {
        // corresponds to process "Free-Format Exponential"
        char[] formattedDecimal = new char[26];
        int charPos = 0;
        if (!positive) {
            formattedDecimal[charPos++] = '-';
        }
        formattedDecimal[charPos++] = (char) ('0' + uArray[getCount++]);
        formattedDecimal[charPos++] = '.';

        int k = firstK;
        int expt = k;
        while (true) {
            k--;
            if (getCount >= setCount) {
                break;
            }
            formattedDecimal[charPos++] = (char) ('0' + uArray[getCount++]);
        }

        if (k == expt - 1) {
            formattedDecimal[charPos++] = '0';
        }
        formattedDecimal[charPos++] = 'E';
        return new String(formattedDecimal, 0, charPos) + Integer.toString(expt);
    }

    private String freeFormat(boolean positive) {
        // corresponds to process "Free-Format"
        char[] formattedDecimal = new char[26];
        // the position the next character is to be inserted into
        // formattedDecimal
        int charPos = 0;
        if (!positive) {
            formattedDecimal[charPos++] = '-';
        }
        int k = firstK;
        if (k < 0) {
            formattedDecimal[charPos++] = '0';
            formattedDecimal[charPos++] = '.';
            for (int i = k + 1; i < 0; ++i) {
                formattedDecimal[charPos++] = '0';
            }
        }

        int U = uArray[getCount++];
        do {
            if (U != -1) {
                formattedDecimal[charPos++] = (char) ('0' + U);
            } else if (k >= -1) {
                formattedDecimal[charPos++] = '0';
            }
            if (k == 0) {
                formattedDecimal[charPos++] = '.';
            }
            k--;
            U = getCount < setCount ? uArray[getCount++] : -1;
        } while (U != -1 || k >= -1);
        return new String(formattedDecimal, 0, charPos);
    }

    private native void bigIntDigitGenerator(long f, int e, boolean isDenormalized, int p);

    private void longDigitGenerator(long f, int e, boolean isDenormalized,
            boolean mantissaIsZero, int p) {
        long R, S, M;
        if (e >= 0) {
            M = 1l << e;
            if (!mantissaIsZero) {
                R = f << (e + 1);
                S = 2;
            } else {
                R = f << (e + 2);
                S = 4;
            }
        } else {
            M = 1;
            if (isDenormalized || !mantissaIsZero) {
                R = f << 1;
                S = 1l << (1 - e);
            } else {
                R = f << 2;
                S = 1l << (2 - e);
            }
        }

        int k = (int) Math.ceil((e + p - 1) * invLogOfTenBaseTwo - 1e-10);

        if (k > 0) {
            S = S * MathUtils.LONG_POWERS_OF_TEN[k];
        } else if (k < 0) {
            long scale = MathUtils.LONG_POWERS_OF_TEN[-k];
            R = R * scale;
            M = M == 1 ? scale : M * scale;
        }

        if (R + M > S) { // was M_plus
            firstK = k;
        } else {
            firstK = k - 1;
            R = R * 10;
            M = M * 10;
        }

        getCount = setCount = 0; // reset indices
        boolean low, high;
        int U;
        long[] Si = new long[] { S, S << 1, S << 2, S << 3 };
        while (true) {
            // set U to be floor (R / S) and R to be the remainder
            // using a kind of "binary search" to find the answer.
            // It's a lot quicker than actually dividing since we know
            // the answer will be between 0 and 10
            U = 0;
            long remainder;
            for (int i = 3; i >= 0; i--) {
                remainder = R - Si[i];
                if (remainder >= 0) {
                    R = remainder;
                    U += 1 << i;
                }
            }

            low = R < M; // was M_minus
            high = R + M > S; // was M_plus

            if (low || high) {
                break;
            }
            R = R * 10;
            M = M * 10;
            uArray[setCount++] = U;
        }
        if (low && !high) {
            uArray[setCount++] = U;
        } else if (high && !low) {
            uArray[setCount++] = U + 1;
        } else if ((R << 1) < S) {
            uArray[setCount++] = U;
        } else {
            uArray[setCount++] = U + 1;
        }
    }
}
