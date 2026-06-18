# override some setup.mk defines
# These might break modern ndk-build, especially with Clang

#override TARGET_arm_release_CFLAGS :=
#override TARGET_thumb_release_CFLAGS :=
#override TARGET_arm_debug_CFLAGS :=
#override TARGET_thumb_debug_CFLAGS :=
#override TARGET_CFLAGS :=

include $(call all-subdir-makefiles)
