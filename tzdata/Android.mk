# Copyright (C) 2015 The Android Open Source Project
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

LOCAL_PATH:= $(call my-dir)

# Subprojects with separate makefiles
subdirs := update_test_app
subdir_makefiles := $(call all-named-subdir-makefiles,$(subdirs))

# Library of tools classes for tzdata updates. Not required on device, except in tests.
include $(CLEAR_VARS)
LOCAL_MODULE := tzdata_tools
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, tools/src/main)
LOCAL_JAVACFLAGS := -encoding UTF-8
LOCAL_STATIC_JAVA_LIBRARIES := tzdata_update
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk
include $(BUILD_STATIC_JAVA_LIBRARY)

# Library of support classes for tzdata updates. Shared between update generation and
# on-device code.
include $(CLEAR_VARS)
LOCAL_MODULE := tzdata_update
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, update/src/main)
LOCAL_JAVACFLAGS := -encoding UTF-8
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk
include $(BUILD_STATIC_JAVA_LIBRARY)

# Tests for tzdata_update code
include $(CLEAR_VARS)
LOCAL_MODULE := tzdata_update-tests
LOCAL_MODULE_TAGS := optional
LOCAL_SRC_FILES := $(call all-java-files-under, update/src/test)
LOCAL_JAVACFLAGS := -encoding UTF-8
LOCAL_STATIC_JAVA_LIBRARIES := tzdata_update tzdata_tools
LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk
include $(BUILD_STATIC_JAVA_LIBRARY)

include $(subdir_makefiles)
