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

#if !defined(hycomp_h)
#define hycomp_h

#if !defined(LINUX)
#define LINUX 1
#endif

/**
 * LITTLE_ENDIAN:          This is for the intel machines or other
 *                         little endian processors. Defaults to big endian.
 *
 * ATOMIC_FLOAT_ACCESS:    So that float operations will work.
 */

 /**
 * By default order doubles in the native (that is big/little endian) ordering.
 */

#define HY_PLATFORM_DOUBLE_ORDER

/**
 * Define common types:
 * <ul>
 * <li><code>U_32 / I_32</code>  - unsigned/signed 32 bits</li>
 * </ul>
 */

typedef          int   I_32;
typedef unsigned int   U_32;

/**
 * Define platform specific types:
 * <ul>
 * <li><code>U_64 / I_64</code>  - unsigned/signed 64 bits</li>
 * </ul>
 */

#if defined(LINUX) || defined(FREEBSD) || defined(AIX)

#define DATA_TYPES_DEFINED

        typedef unsigned long long U_64;
        typedef          long long I_64;

#endif

typedef I_32 IDATA;
typedef U_32 UDATA;

#if !defined(DATA_TYPES_DEFINED)
/* no generic U_64 or I_64 */

#ifndef HY_BIG_ENDIAN
#define HY_LITTLE_ENDIAN
#endif

#endif

#define U32(x)      ((U_32) (x))
#define I32(x)      ((I_32) (x))
#define U32P(x)     ((U_32 *) (x))

#endif /* hycomp_h */
