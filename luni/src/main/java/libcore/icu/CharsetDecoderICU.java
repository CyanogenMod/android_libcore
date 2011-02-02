/**
*******************************************************************************
* Copyright (C) 1996-2006, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
*******************************************************************************
*/
 /**
  * A JNI interface for ICU converters.
  *
  *
  * @author Ram Viswanadha, IBM
  */
package libcore.icu;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import libcore.base.EmptyArray;

public final class CharsetDecoderICU extends CharsetDecoder {
    private static final int MAX_CHARS_PER_BYTE = 2;

    private static final int INPUT_OFFSET = 0;
    private static final int OUTPUT_OFFSET = 1;
    private static final int INVALID_BYTES = 2;
    private static final int INPUT_HELD = 3;
    /*
     * data[INPUT_OFFSET]   = on input contains the start of input and on output the number of input bytes consumed
     * data[OUTPUT_OFFSET]  = on input contains the start of output and on output the number of output chars written
     * data[INVALID_BYTES]  = number of invalid bytes
     * data[INPUT_HELD]     = number of input bytes held in the converter's state
     */
    private int[] data = new int[4];

    /* handle to the ICU converter that is opened */
    private long converterHandle = 0;

    private byte[] input = null;
    private char[] output= null;

    private byte[] allocatedInput = null;
    private char[] allocatedOutput = null;

    // These instance variables are always assigned in the methods before being used. This class
    // is inherently thread-unsafe so we don't have to worry about synchronization.
    private int inEnd;
    private int outEnd;
    private int ec;
    private int savedInputHeldLen;

    public static CharsetDecoderICU newInstance(Charset cs, String icuCanonicalName) {
        // This complexity is necessary to ensure that even if the constructor, superclass
        // constructor, or call to updateCallback throw, we still free the native peer.
        long address = 0;
        try {
            address = NativeConverter.openConverter(icuCanonicalName);
            float averageCharsPerByte = NativeConverter.getAveCharsPerByte(address);
            CharsetDecoderICU result = new CharsetDecoderICU(cs, averageCharsPerByte, address);
            address = 0; // CharsetDecoderICU has taken ownership; its finalizer will do the free.
            result.updateCallback();
            return result;
        } finally {
            if (address != 0) {
                NativeConverter.closeConverter(address);
            }
        }
    }

    private CharsetDecoderICU(Charset cs, float averageCharsPerByte, long address) {
        super(cs, averageCharsPerByte, MAX_CHARS_PER_BYTE);
        this.converterHandle = address;
    }

    /**
     * Sets this decoders replacement string. Substitutes the string in input if an
     * unmappable or illegal sequence is encountered
     * @param newReplacement to replace the error bytes with
     * @stable ICU 2.4
     */
    protected void implReplaceWith(String newReplacement) {
        if (converterHandle > 0) {
            if (newReplacement.length() > NativeConverter.getMaxBytesPerChar(converterHandle)) {
                throw new IllegalArgumentException();
            }
            updateCallback();
        }
     }

    /**
     * Sets the action to be taken if an illegal sequence is encountered
     * @param newAction action to be taken
     * @exception IllegalArgumentException
     * @stable ICU 2.4
     */
    protected final void implOnMalformedInput(CodingErrorAction newAction) {
        updateCallback();
    }

    /**
     * Sets the action to be taken if an illegal sequence is encountered
     * @param newAction action to be taken
     * @exception IllegalArgumentException
     * @stable ICU 2.4
     */
    protected final void implOnUnmappableCharacter(CodingErrorAction newAction) {
        updateCallback();
    }

    private void updateCallback() {
        ec = NativeConverter.setCallbackDecode(converterHandle, this);
        if (ErrorCode.isFailure(ec)) {
            throw ErrorCode.throwException(ec);
        }
    }

    @Override protected final CoderResult implFlush(CharBuffer out) {
        try {
            // ICU needs to see an empty input.
            input = EmptyArray.BYTE;
            inEnd = 0;
            data[INPUT_OFFSET] = 0;

            data[OUTPUT_OFFSET] = getArray(out);
            data[INVALID_BYTES] = 0; // Make sure we don't see earlier errors.

            ec = NativeConverter.decode(converterHandle, input, inEnd, output, outEnd, data, true);
            if (ErrorCode.isFailure(ec)) {
                if (ec == ErrorCode.U_BUFFER_OVERFLOW_ERROR) {
                    return CoderResult.OVERFLOW;
                } else if (ec == ErrorCode.U_TRUNCATED_CHAR_FOUND) {//CSDL: add this truncated character error handling
                    if (data[INPUT_OFFSET] > 0) {
                        return CoderResult.malformedForLength(data[INPUT_OFFSET]);
                    }
                } else {
                    throw ErrorCode.throwException(ec);
                }
            }
            return CoderResult.UNDERFLOW;
       } finally {
            /* save the flushed data */
            setPosition(out);
            implReset();
       }
    }

    @Override protected void implReset() {
        NativeConverter.resetByteToChar(converterHandle);
        data[INPUT_OFFSET] = 0;
        data[OUTPUT_OFFSET] = 0;
        data[INVALID_BYTES] = 0;
        data[INPUT_HELD] = 0;
        savedInputHeldLen = 0;
        output = null;
        input = null;
        allocatedInput = null;
        allocatedOutput = null;
        ec = 0;
        inEnd = 0;
        outEnd = 0;
    }

    @Override protected CoderResult decodeLoop(ByteBuffer in, CharBuffer out) {
        if (!in.hasRemaining()) {
            return CoderResult.UNDERFLOW;
        }

        data[INPUT_OFFSET] = getArray(in);
        data[OUTPUT_OFFSET]= getArray(out);
        data[INPUT_HELD] = 0;

        try{
            ec = NativeConverter.decode(converterHandle, input, inEnd, output, outEnd, data, false);

            // Return an error.
            if (ec == ErrorCode.U_BUFFER_OVERFLOW_ERROR) {
                return CoderResult.OVERFLOW;
            } else if (ec == ErrorCode.U_INVALID_CHAR_FOUND) {
                return CoderResult.unmappableForLength(data[INVALID_BYTES]);
            } else if (ec == ErrorCode.U_ILLEGAL_CHAR_FOUND) {
                return CoderResult.malformedForLength(data[INVALID_BYTES]);
            }
            // Decoding succeeded: give us more data.
            return CoderResult.UNDERFLOW;
        } finally {
            setPosition(in);
            setPosition(out);
        }
    }

    @Override protected void finalize() throws Throwable {
        try {
            NativeConverter.closeConverter(converterHandle);
            converterHandle = 0;
        } finally {
            super.finalize();
        }
    }

    private int getArray(CharBuffer out) {
        if (out.hasArray()) {
            output = out.array();
            outEnd = out.arrayOffset() + out.limit();
            return out.arrayOffset() + out.position();
        } else {
            outEnd = out.remaining();
            if (allocatedOutput == null || (outEnd > allocatedOutput.length)) {
                allocatedOutput = new char[outEnd];
            }
            output = allocatedOutput;
            //since the new
            // buffer start position
            // is 0
            return 0;
        }
    }

    private  int getArray(ByteBuffer in) {
        if (in.hasArray()) {
            input = in.array();
            inEnd = in.arrayOffset() + in.limit();
            return in.arrayOffset() + in.position() + savedInputHeldLen;/*exclude the number fo bytes held in previous conversion*/
        } else {
            inEnd = in.remaining();
            if (allocatedInput == null || (inEnd > allocatedInput.length)) {
                allocatedInput = new byte[inEnd];
            }
            input = allocatedInput;
            // save the current position
            int pos = in.position();
            in.get(input,0,inEnd);
            // reset the position
            in.position(pos);
            // the start position
            // of the new buffer
            // is whatever is savedInputLen
            return savedInputHeldLen;
        }
    }

    private void setPosition(CharBuffer out) {
        if (out.hasArray()) {
            out.position(out.position() + data[OUTPUT_OFFSET] - out.arrayOffset());
        } else {
            out.put(output, 0, data[OUTPUT_OFFSET]);
        }
        // release reference to output array, which may not be ours
        output = null;
    }

    private void setPosition(ByteBuffer in) {
        // ok was there input held in the previous invocation of decodeLoop
        // that resulted in output in this invocation?
        in.position(in.position() + data[INPUT_OFFSET] + savedInputHeldLen - data[INPUT_HELD]);
        savedInputHeldLen = data[INPUT_HELD];
        // release reference to input array, which may not be ours
        input = null;
    }
}
