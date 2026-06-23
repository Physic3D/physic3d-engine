XASH_64BIT ?= 1
XASH_SDL ?= 0
XASH_VGUI ?= 1
XASH_THREAD_NUM ?= 0

APP_PLATFORM := android-24

CFLAGS_OPT := -O3 -fomit-frame-pointer -ggdb -funsafe-math-optimizations -ftree-vectorize -pipe
CFLAGS_OPT_ARM := -mthumb -mfpu=neon -pipe -DVECTORIZE_SINCOS -fPIC -DHAVE_EFFICIENT_UNALIGNED_ACCESS
CFLAGS_OPT_ARM64 := -pipe
CFLAGS_OPT_X86_64 := -pipe -funroll-loops
CFLAGS_OPT_X86 := -mssse3 -mfpmath=sse -funroll-loops -pipe -DVECTORIZE_SINCOS -DHAVE_EFFICIENT_UNALIGNED_ACCESS
CFLAGS_HARDFP := -D_NDK_MATH_NO_SOFTFP=1 -mhard-float -mfloat-abi=hard -DLOAD_HARDFP -DSOFTFP_LINK

APPLICATIONMK_PATH := $(patsubst %/,%,$(dir $(lastword $(MAKEFILE_LIST))))

APP_CFLAGS += -D__ANDROID__
APP_LDFLAGS += -Wl,--no-undefined

NANOGL_PATH := $(APPLICATIONMK_PATH)/../nanogl
XASH3D_PATH := $(APPLICATIONMK_PATH)/../..
HLSDK_PATH  := $(APPLICATIONMK_PATH)/../hlsdk
XASH3D_CONFIG := $(APPLICATIONMK_PATH)/xash3d_config.mk

ifeq ($(XASH_64BIT),1)
APP_ABI := arm64-v8a armeabi-v7a
else
APP_ABI := x86 armeabi-v7a
endif

APP_MODULES :=
