# -*- mode: makefile -*-
# List of libcore directories to include in documentation.
# Shared between libcore and frameworks/base.
# Exports: libcore_to_document as a list of .java files relative to libcore/.

ifndef libcore_docs_include_once

include libcore/openjdk_java_files.mk
include libcore/non_openjdk_java_files.mk


# List of libcore javadoc source files
#
# Note dalvik/system is non-recursive to exclude dalvik.system.profiler
libcore_to_document := $(openjdk_javadoc_files) $(non_openjdk_javadoc_files)

libcore_docs_include_once := 1
endif # libcore_docs_include_once
