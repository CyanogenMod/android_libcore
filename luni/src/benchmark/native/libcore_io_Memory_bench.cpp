/*
 * Copyright (C) 2015 The Android Open Source Project
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

// The functions we want to benchmark are static, so include the source code.
#include "luni/src/main/native/libcore_io_Memory.cpp"

#include <benchmark/Benchmark.h>

template<typename T, size_t ALIGN>
void swap_bench(testing::Benchmark* bench, void (*swap_func)(T*, const T*, size_t),
                int iters, size_t num_elements) {
  T* src;
  T* dst;
  T* src_elems;
  T* dst_elems;

  if (ALIGN) {
    src_elems = new T[num_elements + 1];
    dst_elems = new T[num_elements + 1];

    src = reinterpret_cast<T*>(reinterpret_cast<uintptr_t>(src_elems) + ALIGN);
    dst = reinterpret_cast<T*>(reinterpret_cast<uintptr_t>(dst_elems) + ALIGN);
  } else {
    src_elems = new T[num_elements];
    dst_elems = new T[num_elements];

    src = src_elems;
    dst = dst_elems;
  }

  memset(dst, 0, sizeof(T) * num_elements);
  memset(src, 0x12, sizeof(T) * num_elements);

  bench->StartBenchmarkTiming();

  for (int i = 0; i < iters; i++) {
    swap_func(src, dst, num_elements);
  }

  bench->StopBenchmarkTiming();

  delete[] src_elems;
  delete[] dst_elems;
}

#define AT_COMMON_VALUES \
    Arg(10)->Arg(100)->Arg(1000)->Arg(1024*10)->Arg(1024*100)

BENCHMARK_WITH_ARG(BM_libcore_swapShorts_aligned, int)->AT_COMMON_VALUES;
void BM_libcore_swapShorts_aligned::Run(int iters, int num_shorts) {
  swap_bench<jshort, 0>(this, swapShorts, iters, num_shorts);
}

BENCHMARK_WITH_ARG(BM_libcore_swapInts_aligned, int)->AT_COMMON_VALUES;
void BM_libcore_swapInts_aligned::Run(int iters, int num_ints) {
  swap_bench<jint, 0>(this, swapInts, iters, num_ints);
}

BENCHMARK_WITH_ARG(BM_libcore_swapLongs_aligned, int)->AT_COMMON_VALUES;
void BM_libcore_swapLongs_aligned::Run(int iters, int num_longs) {
  swap_bench<jlong, 0>(this, swapLongs, iters, num_longs);
}

BENCHMARK_WITH_ARG(BM_libcore_swapShorts_unaligned1, int)->AT_COMMON_VALUES;
void BM_libcore_swapShorts_unaligned1::Run(int iters, int num_shorts) {
  swap_bench<jshort, 1>(this, swapShorts, iters, num_shorts);
}

BENCHMARK_WITH_ARG(BM_libcore_swapInts_unaligned1, int)->AT_COMMON_VALUES;
void BM_libcore_swapInts_unaligned1::Run(int iters, int num_ints) {
  swap_bench<jint, 1>(this, swapInts, iters, num_ints);
}

BENCHMARK_WITH_ARG(BM_libcore_swapLongs_unaligned1, int)->AT_COMMON_VALUES;
void BM_libcore_swapLongs_unaligned1::Run(int iters, int num_longs) {
  swap_bench<jlong, 1>(this, swapLongs, iters, num_longs);
}

BENCHMARK_WITH_ARG(BM_libcore_swapShorts_unaligned2, int)->AT_COMMON_VALUES;
void BM_libcore_swapShorts_unaligned2::Run(int iters, int num_shorts) {
  swap_bench<jshort, 2>(this, swapShorts, iters, num_shorts);
}

BENCHMARK_WITH_ARG(BM_libcore_swapInts_unaligned2, int)->AT_COMMON_VALUES;
void BM_libcore_swapInts_unaligned2::Run(int iters, int num_ints) {
  swap_bench<jint, 2>(this, swapInts, iters, num_ints);
}

BENCHMARK_WITH_ARG(BM_libcore_swapLongs_unaligned2, int)->AT_COMMON_VALUES;
void BM_libcore_swapLongs_unaligned2::Run(int iters, int num_longs) {
  swap_bench<jlong, 2>(this, swapLongs, iters, num_longs);
}
