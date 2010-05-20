/**
*******************************************************************************
* Copyright (C) 1996-2005, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/

package com.ibm.icu4jni.charset;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Map;

public final class CharsetICU extends Charset {
    private static final Map<String, byte[]> DEFAULT_REPLACEMENTS = new HashMap<String, byte[]>();
    static {
        // ICU has different default replacements to the RI in these cases. There are probably
        // more cases too, but this covers all the charsets that Java guarantees will be available.
        // These use U+FFFD REPLACEMENT CHARACTER...
        DEFAULT_REPLACEMENTS.put("UTF-16",   new byte[] { (byte) 0xff, (byte) 0xfd });
        DEFAULT_REPLACEMENTS.put("UTF-32",   new byte[] { (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xfd });
        // These use '?'. It's odd that UTF-8 doesn't use U+FFFD, given that (unlike ISO-8859-1
        // and US-ASCII) it can represent it, but this is what the RI does...
        byte[] questionMark = new byte[] { (byte) '?' };
        DEFAULT_REPLACEMENTS.put("UTF-8",      questionMark);
        DEFAULT_REPLACEMENTS.put("ISO-8859-1", questionMark);
        DEFAULT_REPLACEMENTS.put("US-ASCII",   questionMark);
    }

    private final String icuCanonicalName;

    protected CharsetICU(String canonicalName, String icuCanonName, String[] aliases) {
         super(canonicalName, aliases);
         icuCanonicalName = icuCanonName;
    }

    public CharsetDecoder newDecoder() {
        return new CharsetDecoderICU(this, NativeConverter.openConverter(icuCanonicalName));
    }

    public CharsetEncoder newEncoder() {
        long converterHandle = NativeConverter.openConverter(icuCanonicalName);
        // We have our own map of RI-compatible default replacements...
        byte[] replacement = DEFAULT_REPLACEMENTS.get(icuCanonicalName);
        if (replacement == null) {
            // ...but fall back to asking ICU.
            // TODO: should we just try to use U+FFFD and fall back to '?' if U+FFFD can't be encoded?
            replacement = NativeConverter.getSubstitutionBytes(converterHandle);
        } else {
            replacement = replacement.clone();
        }
        return new CharsetEncoderICU(this, converterHandle, replacement);
    }

    public boolean contains(Charset cs) {
        if (cs == null) {
            return false;
        } else if (this.equals(cs)) {
            return true;
        }

        long converterHandle1 = 0;
        long converterHandle2 = 0;

        try {
            converterHandle1 = NativeConverter.openConverter(this.name());
            if (converterHandle1 > 0) {
                converterHandle2 = NativeConverter.openConverter(cs.name());
                if (converterHandle2 > 0) {
                    return NativeConverter.contains(converterHandle1, converterHandle2);
                }
            }
            return false;
        } finally {
            if (0 != converterHandle1) {
                NativeConverter.closeConverter(converterHandle1);
                if (0 != converterHandle2) {
                    NativeConverter.closeConverter(converterHandle2);
                }
            }
        }
    }
}
