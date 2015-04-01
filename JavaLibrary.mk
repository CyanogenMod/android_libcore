# -*- mode: makefile -*-
# Copyright (C) 2007 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# Definitions for building the Java library and associated tests.
#

#
# Common definitions for host and target.
#

# libcore is divided into modules.
#
# The structure of each module is:
#
#   src/
#       main/               # To be shipped on every device.
#            java/          # Java source for library code.
#            native/        # C++ source for library code.
#            resources/     # Support files.
#       test/               # Built only on demand, for testing.
#            java/          # Java source for tests.
#            native/        # C++ source for tests (rare).
#            resources/     # Support files.
#
# All subdirectories are optional (hence the "2> /dev/null"s below).

define all-main-java-files-under
$(foreach dir,$(1),$(patsubst ./%,%,$(shell cd $(LOCAL_PATH) && find $(dir)/src/main/java -name "*.java" 2> /dev/null)))
endef

define all-test-java-files-under
$(foreach dir,$(1),$(patsubst ./%,%,$(shell cd $(LOCAL_PATH) && find $(dir)/src/test/java -name "*.java" 2> /dev/null)))
endef

define all-core-resource-dirs
$(shell cd $(LOCAL_PATH) && ls -d */src/$(1)/{java,resources} 2> /dev/null)
endef

# The Java files and their associated resources.
common_core_src_files := $(call all-main-java-files-under,dalvik dex dom json luni xml)
core_resource_dirs := $(call all-core-resource-dirs,main)
test_resource_dirs := $(call all-core-resource-dirs,test)
test_src_files := $(call all-test-java-files-under,dalvik dom harmony-tests json luni xml)

ifeq ($(EMMA_INSTRUMENT),true)
ifneq ($(EMMA_INSTRUMENT_STATIC),true)
    common_core_src_files += $(call all-java-files-under, ../external/emma/core ../external/emma/pregenerated)
    core_resource_dirs += ../external/emma/core/res ../external/emma/pregenerated/res
endif
endif

libart_core_src_files += $(common_core_src_files) $(call all-main-java-files-under,libart)

local_javac_flags=-encoding UTF-8
#local_javac_flags+=-Xlint:all -Xlint:-serial,-deprecation,-unchecked
local_javac_flags+=-Xmaxwarns 9999999

#
# ICU4J related rules.
#
# We compile icu4j along with core-libart because we're implementing parts of core-libart
# in terms of icu4j.
icu4j_root := ../external/icu/icu4j/
icu4j_src_files := $(call all-java-files-under,$(icu4j_root)/main/classes)

# Filter out bits of ICU4J we don't use yet : the SPIs (which we have limited support for),
# the charset encoders and the transliterators.
icu4j_src_files := $(filter-out $(icu4j_root)/main/classes/localespi/%, $(icu4j_src_files))
icu4j_src_files := $(filter-out $(icu4j_root)/main/classes/charset/%, $(icu4j_src_files))
icu4j_src_files := $(filter-out $(icu4j_root)/main/classes/translit/%, $(icu4j_src_files))

# Not all src dirs contain resources, some instead contain other random files
# that should not be included as resources. The ones that should be included
# can be identifed by the fact that they contain particular subdir trees.
#
define all-icu-subdir-with-subdir
$(patsubst $(LOCAL_PATH)/%/$(2),%,$(wildcard $(LOCAL_PATH)/$(1)/$(2)))
endef

icu4j_resource_dirs := $(call all-icu-subdir-with-subdir,$(icu4j_root)/main/classes/*/src,com/ibm/icu)
icu4j_resource_dirs := $(filter-out $(icu4j_root)/main/classes/localespi/%, $(icu4j_resource_dirs))
icu4j_resource_dirs := $(filter-out $(icu4j_root)/main/classes/charset/%, $(icu4j_resource_dirs))
icu4j_resource_dirs := $(filter-out $(icu4j_root)/main/classes/translit/%, $(icu4j_resource_dirs))



#
# Build for the target (device).
#

# Definitions to make the core library.

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(libart_core_src_files) $(icu4j_src_files)
LOCAL_JAVA_RESOURCE_DIRS := $(core_resource_dirs) $(icu4j_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_DX_FLAGS := --core-library
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := core-libart
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
LOCAL_REQUIRED_MODULES := tzdata
LOCAL_JARJAR_RULES := $(LOCAL_PATH)/jarjar-rules.txt
include $(BUILD_JAVA_LIBRARY)

# Path to the ICU4C data files in the Android device file system:
icu4c_data := /system/usr/icu
# TODO: It's quite hideous that this double-slash between icu4j and main is required.
# It's because we provide a variable substition of the make-rule generated jar command
# to substitute a processed ICUProperties.config file in place of the original.
#
# We can avoid this by filtering out ICUConfig.properties from our list of resources.
icu4j_config_root := $(LOCAL_PATH)/../external/icu/icu4j//main/classes/core/src
include external/icu/icu4j/adjust_icudt_path.mk

ifeq ($(LIBCORE_SKIP_TESTS),)
# Make the core-tests library.
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(test_src_files)
LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := core-libart okhttp core-junit bouncycastle
LOCAL_STATIC_JAVA_LIBRARIES := core-tests-support sqlite-jdbc mockwebserver nist-pkix-tests
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE := core-tests
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
include $(BUILD_STATIC_JAVA_LIBRARY)
endif

ifeq ($(LIBCORE_SKIP_TESTS),)
# Make the core-tests-support library.
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-test-java-files-under,support)
LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := core-libart core-junit bouncycastle
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE := core-tests-support
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
include $(BUILD_STATIC_JAVA_LIBRARY)
endif

ifeq ($(LIBCORE_SKIP_TESTS),)
# Make the jsr166-tests library.
include $(CLEAR_VARS)
LOCAL_SRC_FILES :=  $(call all-test-java-files-under, jsr166-tests)
LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVA_LIBRARIES := core-libart core-junit
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE := jsr166-tests
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
include $(BUILD_STATIC_JAVA_LIBRARY)
endif

#
# Build for the host.
#

ifeq ($(HOST_OS),linux)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(call all-main-java-files-under, dex)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := dex-host
include $(BUILD_HOST_JAVA_LIBRARY)

# Definitions to make the core library.
include $(CLEAR_VARS)
LOCAL_SRC_FILES := $(libart_core_src_files) $(icu4j_src_files)
LOCAL_JAVA_RESOURCE_DIRS := $(core_resource_dirs) $(icu4j_resource_dirs)
LOCAL_NO_STANDARD_LIBRARIES := true
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_DX_FLAGS := --core-library
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := core-libart-hostdex
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
LOCAL_REQUIRED_MODULES := tzdata-host
LOCAL_JARJAR_RULES := $(LOCAL_PATH)/jarjar-rules.txt
include $(BUILD_HOST_DALVIK_JAVA_LIBRARY)

# Make the core-tests library.
ifeq ($(LIBCORE_SKIP_TESTS),)
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(test_src_files)
    LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
    LOCAL_NO_STANDARD_LIBRARIES := true
    LOCAL_JAVA_LIBRARIES := core-libart-hostdex okhttp-hostdex bouncycastle-hostdex core-junit-hostdex core-tests-support-hostdex
    LOCAL_STATIC_JAVA_LIBRARIES := sqlite-jdbc-host mockwebserver-host nist-pkix-tests-host
    LOCAL_JAVACFLAGS := $(local_javac_flags)
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := core-tests-hostdex
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
    include $(BUILD_HOST_DALVIK_JAVA_LIBRARY)
endif

# Make the core-tests-support library.
ifeq ($(LIBCORE_SKIP_TESTS),)
    include $(CLEAR_VARS)
    LOCAL_SRC_FILES := $(call all-test-java-files-under,support)
    LOCAL_JAVA_RESOURCE_DIRS := $(test_resource_dirs)
    LOCAL_NO_STANDARD_LIBRARIES := true
    LOCAL_JAVA_LIBRARIES := core-libart-hostdex core-junit-hostdex bouncycastle-hostdex
    LOCAL_JAVACFLAGS := $(local_javac_flags)
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := core-tests-support-hostdex
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk
    include $(BUILD_HOST_DALVIK_JAVA_LIBRARY)
endif

endif # HOST_OS == linux

#
# Local droiddoc for faster libcore testing
#
#
# Run with:
#     mm -j32 libcore-docs
#
# Main output:
#     ../out/target/common/docs/libcore/reference/packages.html
#
# All text for proofreading (or running tools over):
#     ../out/target/common/docs/libcore-proofread.txt
#
# TODO list of missing javadoc, etc:
#     ../out/target/common/docs/libcore-docs-todo.html
#
# Rerun:
#     rm -rf ../out/target/common/docs/libcore-timestamp && mm -j32 libcore-docs
#
include $(CLEAR_VARS)

# for shared defintion of libcore_to_document
include $(LOCAL_PATH)/Docs.mk

LOCAL_SRC_FILES := $(libcore_to_document)
# rerun doc generation without recompiling the java
LOCAL_JAVA_LIBRARIES:=
LOCAL_JAVACFLAGS := $(local_javac_flags)
LOCAL_MODULE_CLASS:=JAVA_LIBRARIES

LOCAL_MODULE := libcore
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/JavaLibrary.mk

LOCAL_DROIDDOC_OPTIONS := \
 -offlinemode \
 -title "libcore" \
 -proofread $(OUT_DOCS)/$(LOCAL_MODULE)-proofread.txt \
 -todo ../$(LOCAL_MODULE)-docs-todo.html \
 -hdf android.whichdoc offline

LOCAL_DROIDDOC_CUSTOM_TEMPLATE_DIR:=build/tools/droiddoc/templates-sdk

include $(BUILD_DROIDDOC)
