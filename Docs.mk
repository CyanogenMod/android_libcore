# -*- mode: makefile -*-
# List of libcore directories to include in documentation.
# Shared between libcore and frameworks/base.
# Exports: libcore_to_document as a list of .java files relative to libcore/.

ifndef libcore_docs_include_once

# List of libcore javadoc source files
#
# Note dalvik/system is non-recursive to exclude dalvik.system.profiler
libcore_to_document := \
 $(call find-files-in-subdirs, libcore, \
   "*.java", \
   dalvik/src/main/java/dalvik/system/ -maxdepth 1) \
 $(call find-files-in-subdirs, libcore, \
   "*.java", \
   dalvik/src/main/java/dalvik/annotation \
   dalvik/src/main/java/dalvik/bytecode \
   json/src/main/java \
   libart/src/main/java/dalvik \
   libart/src/main/java/java \
   luni/src/main/java/android \
   luni/src/main/java/javax \
   luni/src/main/java/org/xml/sax \
   luni/src/main/java/org/w3c \
   xml/src/main/java/org/xmlpull/v1)

# IcuIteratorWrapper.java references com.ibm.icu.text.BreakIterator,
# which is renamed by our jarjar rule, and so unrecognizable by javadoc,
# with annoying error: error: package com.ibm.icu.text does not exist.
# We don't want to generate doc for this file anyway.
libcore_to_document += \
  $(filter-out luni/src/main/java/java/text/IcuIteratorWrapper.java,\
    $(call find-files-in-subdirs, libcore, \
      "*.java", \
      luni/src/main/java/java))

libcore_docs_include_once := 1
endif # libcore_docs_include_once
