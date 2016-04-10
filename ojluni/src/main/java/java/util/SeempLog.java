/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of The Linux Foundation nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package java.util;

/*
import com.android.internal.os.RuntimeInit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
*/

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @hide
 */
public final class SeempLog {

    private static Method seemp_record_method = null;
    private static boolean seemp_record_method_looked_up = false;

    private SeempLog() {
    }

    /**
     * Send a log message to the seemp log.
     * @param msg The message you would like logged.
     */
    public static int record_str(int api, String msg) {
        if (seemp_record_method == null) {
            if (!seemp_record_method_looked_up) {
                try
                {
                    Class c = Class.forName( "android.util.SeempLog" );
                    if (c!=null) {
                        seemp_record_method = c.getDeclaredMethod( "record_str", int.class, String.class );
                    }
                } catch (ClassNotFoundException ex) {
                    seemp_record_method = null;
                } catch (NoSuchMethodException ex) {
                    seemp_record_method = null;
                }

            }
            seemp_record_method_looked_up = true;
        }

        if (seemp_record_method!=null) {
            try {
                return ( ( Integer )seemp_record_method.invoke( null, api, msg ) ).intValue();
            } catch  (IllegalAccessException ex) {
                return 0;
            } catch (InvocationTargetException ex) {
                return 0;
            }
        }

        return 0;// seemp_println_native("-1|" + msg);
    }

}
