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
    private final static double invLogOfTenBaseTwo = Math.log(2.0) / Math.log(10.0);

    private int firstK;

    /**
     * An array of decimal digits, filled by longDigitGenerator or bigIntDigitGenerator.
     */
    private final int[] digits = new int[64];

    /**
     * Number of valid entries in 'digits'.
     */
    private int digitCount;

    private static final ThreadLocal<RealToString> INSTANCE = new ThreadLocal<RealToString>() {
        @Override protected RealToString initialValue() {
            return new RealToString();
        }
    };

    private RealToString() {
    }

    public static RealToString getInstance() {
        return INSTANCE.get();
    }

    public String doubleToString(double inputNumber) {
        long inputNumberBits = Double.doubleToRawLongBits(inputNumber);
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

        firstK = digitCount = 0;
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
        int inputNumberBits = Float.floatToRawIntBits(inputNumber);
        boolean positive = (inputNumberBits & Float.SIGN_MASK) == 0;
        int e = (inputNumberBits & Float.EXPONENT_MASK) >> Float.MANTISSA_BITS;
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

        firstK = digitCount = 0;
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
        int digitIndex = 0;
        StringBuilder sb = new StringBuilder(26);
        if (!positive) {
            sb.append('-');
        }
        sb.append((char) ('0' + digits[digitIndex++]));
        sb.append('.');

        int k = firstK;
        int exponent = k;
        while (true) {
            k--;
            if (digitIndex >= digitCount) {
                break;
            }
            sb.append((char) ('0' + digits[digitIndex++]));
        }

        if (k == exponent - 1) {
            sb.append('0');
        }
        sb.append('E');
        sb.append(exponent);
        return sb.toString();
    }

    private String freeFormat(boolean positive) {
        int digitIndex = 0;
        StringBuilder sb = new StringBuilder(26);
        if (!positive) {
            sb.append('-');
        }
        int k = firstK;
        if (k < 0) {
            sb.append('0');
            sb.append('.');
            for (int i = k + 1; i < 0; ++i) {
                sb.append('0');
            }
        }
        int U = digits[digitIndex++];
        do {
            if (U != -1) {
                sb.append((char) ('0' + U));
            } else if (k >= -1) {
                sb.append('0');
            }
            if (k == 0) {
                sb.append('.');
            }
            k--;
            U = digitIndex < digitCount ? digits[digitIndex++] : -1;
        } while (U != -1 || k >= -1);
        return sb.toString();
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

        boolean low, high;
        int U;
        while (true) {
            U = (int) (R / S);
            R = R - U*S; // Faster than "R = R % S" on nexus one, which only has hardware MUL.

            low = R < M; // was M_minus
            high = R + M > S; // was M_plus

            if (low || high) {
                break;
            }
            R = R * 10;
            M = M * 10;
            digits[digitCount++] = U;
        }
        if (low && !high) {
            digits[digitCount++] = U;
        } else if (high && !low) {
            digits[digitCount++] = U + 1;
        } else if ((R << 1) < S) {
            digits[digitCount++] = U;
        } else {
            digits[digitCount++] = U + 1;
        }
    }
}
