# Copyright (C) 2013 The Android Open Source Project
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

# Static library with some common classes for the phone apps.
# To use it add this line in your Android.mk
#  LOCAL_STATIC_JAVA_LIBRARIES := com.android.telephony.common
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    src/com/android/telephony/services/CallLogAsync.java \
    src/com/android/telephony/services/HapticFeedback.java

LOCAL_MODULE := com.android.telephony.common
include $(BUILD_STATIC_JAVA_LIBRARY)

# Build the Phone app which includes the emergency dialer. See Contacts
# for the 'other' dialer.
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := telephony-common com.android.telephony.client
LOCAL_SRC_FILES := $(call all-java-files-under, src/com/android/telephony/services) \
    $(call all-logtags-files-under, src/com/android/telephony/services) \
    $(call all-Iaidl-files-under, src/com/android/telephony/services)

LOCAL_PACKAGE_NAME := Telephony
LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Build the test package
# TODO: Not moved yet.
# include $(call all-makefiles-under,$(LOCAL_PATH))

#=====================================================
include $(CLEAR_VARS)

LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/src
LOCAL_SRC_FILES := $(call all-java-files-under, src/com/android/telephony/client) \
    $(call all-Iaidl-files-under, src/com/android/telephony/client) \
    $(call all-logtags-files-under, src/com/android/telephony/client)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.android.telephony.client
LOCAL_CERTIFICATE := platform

include $(BUILD_JAVA_LIBRARY)

# ==========================================================================
include $(CLEAR_VARS)
# Module xml file
LOCAL_MODULE := com.android.telephony.client.xml

#Supporting the Module tag user , eng and userdebug
LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := ETC

# This will install the file in /system/etc/permissions
LOCAL_MODULE_PATH := $(TARGET_OUT_ETC)/permissions

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)
