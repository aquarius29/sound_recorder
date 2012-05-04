$(info HELLO) 

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := FFT
LOCAL_SRC_FILES := processRawData.c
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)