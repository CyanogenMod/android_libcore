# -*- mode: makefile -*-
# List of libcore directories to include in documentation.
# Shared between libcore and frameworks/base.
# Exports: libcore_to_document as a list of .java files relative to libcore/.

ifndef libcore_docs_include_once

include libcore/openjdk_java_files.mk
include libcore/non_openjdk_java_files.mk


# List of libcore javadoc source files
libcore_to_document := $(openjdk_javadoc_files) $(non_openjdk_javadoc_files)

# IcuIteratorWrapper.java references com.ibm.icu.text.BreakIterator,
# which is renamed by our jarjar rule, and so unrecognizable by javadoc,
# with annoying error: error: package com.ibm.icu.text does not exist.
# We don't want to generate doc for this file anyway.
#
# TODO(narayan): Deal with this exclusion.
#
# libcore_to_document += \
#   $(filter-out luni/src/main/java/java/text/IcuIteratorWrapper.java,\
#     $(call find-files-in-subdirs, libcore, \
#       "*.java", \
#       luni/src/main/java/java))

libcore_docs_include_once := 1
endif # libcore_docs_include_once
