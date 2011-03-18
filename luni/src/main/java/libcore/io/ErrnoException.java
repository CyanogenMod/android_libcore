/*
 * Copyright (C) 2011 The Android Open Source Project
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

package libcore.io;

import java.io.IOException;
import libcore.io.OsConstants;

/**
 * An unchecked exception thrown when {@link Os} methods fail. This exception contains the native
 * errno value, for comparison against the constants in {@link OsConstants}, should sophisticated
 * callers need to adjust their behavior based on the exact failure.
 */
public final class ErrnoException extends RuntimeException {
    public final int errno;

    public ErrnoException(int errno) {
        this.errno = errno;
    }

    public ErrnoException(int errno, Throwable cause) {
        super(cause);
        this.errno = errno;
    }

    /**
     * Converts the stashed errno to a human-readable string. We do this here rather than in
     * the constructor so that callers only pay for this if they need it.
     */
    @Override public String getMessage() {
        String name = OsConstants.errnoName(errno);
        if (name == null) {
            name = "errno " + errno;
        }
        String description = Libcore.os.strerror(errno);
        return name + " (" + description + ")";
    }

    public IOException rethrowAsIOException(String detail) throws IOException {
        IOException ioException = new IOException(detail);
        ioException.initCause(this);
        throw ioException;
    }
}
