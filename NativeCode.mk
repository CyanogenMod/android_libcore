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
# Definitions for building the native code needed for the core library.
#

#
# Common definitions for host and target.
#

# These two definitions are used to help sanity check what's put in
# sub.mk. See, the "error" directives immediately below.
core_magic_local_target := ...//::default:://...
core_local_path := $(LOCAL_PATH)

# Include a submakefile, resolve its source file locations,
# and stick them on core_src_files.  The submakefiles are
# free to append to LOCAL_SRC_FILES, LOCAL_C_INCLUDES,
# LOCAL_SHARED_LIBRARIES, or LOCAL_STATIC_LIBRARIES, but nothing
# else. All other LOCAL_* variables will be ignored.
#
# $(1): directory containing the makefile to include
define include-core-native-dir
    LOCAL_SRC_FILES :=
    include $(LOCAL_PATH)/$(1)/sub.mk
    ifneq ($$(LOCAL_MODULE),$(core_magic_local_target))
        $$(error $(LOCAL_PATH)/$(1)/sub.mk should not include CLEAR_VARS \
            or define LOCAL_MODULE)
    endif
    ifneq ($$(LOCAL_PATH),$(core_local_path))
        $$(error $(LOCAL_PATH)/$(1)/sub.mk should not define LOCAL_PATH)
    endif
    core_src_files += $$(addprefix $(1)/,$$(LOCAL_SRC_FILES))
    LOCAL_SRC_FILES :=
endef

# Set up the default state. Note: We use CLEAR_VARS here, even though
# we aren't quite defining a new rule yet, to make sure that the
# sub.mk files don't see anything stray from the last rule that was
# set up.

# Set up the test library first
ifeq ($(LIBCORE_SKIP_TESTS),)
include $(CLEAR_VARS)
LOCAL_MODULE := $(core_magic_local_target)
core_src_files :=

# Include the sub.mk files.
$(foreach dir, \
    luni/src/test/native, \
    $(eval $(call include-core-native-dir,$(dir))))

# This is for the test library, so rename the variable.
test_src_files := $(core_src_files)
core_src_files :=

# Extract out the allowed LOCAL_* variables. Note: $(sort) also
# removes duplicates.
test_c_includes := $(sort libcore/include $(LOCAL_C_INCLUDES) $(JNI_H_INCLUDE))
test_shared_libraries := $(sort $(LOCAL_SHARED_LIBRARIES))
test_static_libraries := $(sort $(LOCAL_STATIC_LIBRARIES))
endif # LIBCORE_SKIP_TESTS


include $(CLEAR_VARS)
LOCAL_MODULE := $(core_magic_local_target)
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/NativeCode.mk
core_src_files :=

# Include the sub.mk files.
$(foreach dir, \
    dalvik/src/main/native luni/src/main/native, \
    $(eval $(call include-core-native-dir,$(dir))))

# Extract out the allowed LOCAL_* variables. Note: $(sort) also
# removes duplicates.
core_c_includes := $(sort libcore/include $(LOCAL_C_INCLUDES) $(JNI_H_INCLUDE))
core_shared_libraries := $(sort $(LOCAL_SHARED_LIBRARIES))
core_static_libraries := $(sort $(LOCAL_STATIC_LIBRARIES))


#
# Build for the target (device).
#

include $(CLEAR_VARS)

LOCAL_CFLAGS += -Wall -Wextra -Werror
LOCAL_CFLAGS += $(core_cflags)
LOCAL_CPPFLAGS += $(core_cppflags)
ifeq ($(TARGET_ARCH),arm)
# Ignore "note: the mangling of 'va_list' has changed in GCC 4.4"
LOCAL_CFLAGS += -Wno-psabi
endif

# Define the rules.
LOCAL_SRC_FILES := $(core_src_files)
LOCAL_C_INCLUDES := $(core_c_includes)
LOCAL_SHARED_LIBRARIES := $(core_shared_libraries) libexpat libicuuc libicui18n libssl libcrypto libz libnativehelper
LOCAL_STATIC_LIBRARIES := $(core_static_libraries)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libjavacore
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/NativeCode.mk

LOCAL_C_INCLUDES += external/stlport/stlport bionic/ bionic/libstdc++/include
LOCAL_SHARED_LIBRARIES += libstlport

include $(BUILD_SHARED_LIBRARY)


# Test library
ifeq ($(LIBCORE_SKIP_TESTS),)
include $(CLEAR_VARS)

LOCAL_CFLAGS += -Wall -Wextra -Werror
LOCAL_CFLAGS += $(core_cflags)
LOCAL_CPPFLAGS += $(core_cppflags)
ifeq ($(TARGET_ARCH),arm)
# Ignore "note: the mangling of 'va_list' has changed in GCC 4.4"
LOCAL_CFLAGS += -Wno-psabi
endif

# Define the rules.
LOCAL_SRC_FILES := $(test_src_files)
LOCAL_C_INCLUDES := $(test_c_includes)
LOCAL_SHARED_LIBRARIES := $(test_shared_libraries)
LOCAL_STATIC_LIBRARIES := $(test_static_libraries)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := libjavacoretests
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/NativeCode.mk

include $(BUILD_SHARED_LIBRARY)
endif # LIBCORE_SKIP_TESTS


#
# Build for the host.
#

ifeq ($(WITH_HOST_DALVIK),true)
    include $(CLEAR_VARS)
    # Define the rules.
    LOCAL_SRC_FILES := $(core_src_files)
    LOCAL_CFLAGS += $(core_cflags)
    LOCAL_C_INCLUDES := $(core_c_includes)
    LOCAL_CPPFLAGS += $(core_cppflags)
    LOCAL_LDLIBS += -ldl -lpthread
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := libjavacore
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/NativeCode.mk
    LOCAL_SHARED_LIBRARIES := $(core_shared_libraries) libexpat libicuuc libicui18n libssl libcrypto libz-host
    LOCAL_STATIC_LIBRARIES := $(core_static_libraries)
    include $(BUILD_HOST_SHARED_LIBRARY)

    ifeq ($(LIBCORE_SKIP_TESTS),)
    include $(CLEAR_VARS)
    # Define the rules.
    LOCAL_SRC_FILES := $(test_src_files)
    LOCAL_CFLAGS += $(core_cflags)
    LOCAL_C_INCLUDES := $(test_c_includes)
    LOCAL_CPPFLAGS += $(core_cppflags)
    LOCAL_LDLIBS += -ldl -lpthread
    LOCAL_MODULE_TAGS := optional
    LOCAL_MODULE := libjavacoretests
    LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/NativeCode.mk
    LOCAL_SHARED_LIBRARIES := $(test_shared_libraries)
    LOCAL_STATIC_LIBRARIES := $(test_static_libraries)
    include $(BUILD_HOST_SHARED_LIBRARY)
    endif # LIBCORE_SKIP_TESTS
endif
