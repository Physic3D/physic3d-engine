#Xash Engine Android port
#Copyright (c) nicknekit

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := xash

APP_PLATFORM := android-12

include $(XASH3D_CONFIG)

LOCAL_CFLAGS += -D__MULTITEXTURE_SUPPORT__ -DXASH_GLES -DXASH_NANOGL -DUSE_EVDEV -DXASH_DYNAMIC_DLADDR -DXASH_OPENSL -DXASH_SKIPCRTLIB -DXASH_FORCEINLINE -DXASH_FASTSTR

XASH_COMMIT := $(firstword $(shell cd $(LOCAL_PATH)&&git rev-parse --short=6 HEAD) unknown)

LOCAL_CFLAGS += -DXASH_BUILD_COMMIT=\"$(XASH_COMMIT)\"

LOCAL_CONLYFLAGS += -std=c99

LOCAL_C_INCLUDES := \
	$(NANOGL_PATH)/GL			    \
	$(NANOGL_PATH)/				    \
	$(LOCAL_PATH)/.				    \
	$(LOCAL_PATH)/common			    \
	$(LOCAL_PATH)/client			    \
	$(LOCAL_PATH)/client/vgui		    \
	$(LOCAL_PATH)/server			    \
	$(LOCAL_PATH)/client/imagelib		    \
	$(LOCAL_PATH)/platform/android		    \
	$(LOCAL_PATH)/platform/posix		    \
	$(LOCAL_PATH)/common/soundlib		    \
	$(LOCAL_PATH)/common/soundlib/libmpg        \
	$(LOCAL_PATH)/public		            \
	$(LOCAL_PATH)/pm_shared		    \
	$(LOCAL_PATH)/../			    \
	$(LOCAL_PATH)/../utils/vgui/include	    \
	$(HLSDK_PATH)/cl_dll/

LOCAL_EXPORT_C_INCLUDES := $(LOCAL_C_INCLUDES)

LOCAL_SRC_FILES := \
	   platform/android/dlsym-weak.cpp \
	   client/cl_cmds.cpp \
           client/cl_demo.cpp \
           client/cl_events.cpp \
           client/cl_frame.cpp \
           client/cl_game.cpp \
           client/cl_main.cpp \
           client/cl_menu.cpp \
           client/cl_mobile.cpp \
           client/cl_parse.cpp \
           client/cl_pmove.cpp \
           client/cl_remap.cpp \
           client/cl_scrn.cpp \
           client/cl_tent.cpp \
           client/cl_video.cpp \
           client/cl_view.cpp \
           client/cl_netgraph.cpp \
           client/gl_backend.c \
           client/gl_beams.cpp \
           client/gl_cull.cpp \
           client/gl_decals.cpp \
           client/gl_draw.cpp \
           client/gl_image.cpp \
           client/gl_mirror.cpp \
           client/gl_refrag.cpp \
           client/gl_rlight.cpp \
           client/gl_rmain.cpp \
           client/gl_rmath.cpp \
           client/gl_rmisc.cpp \
           client/gl_rpart.cpp \
           client/gl_rsurf.c \
           client/gl_rstrobe.cpp \
           client/gl_sprite.c \
           client/gl_studio.cpp \
           client/vid_common.cpp \
           client/gl_warp.cpp \
           client/joyinput.cpp \
           platform/android/snd_opensles.cpp \
           client/input.cpp \
           client/keys.cpp \
           client/input_evdevkey.cpp \
           client/console.cpp \
           client/touch.c \
           client/gamma.cpp \
           client/s_dsp.cpp \
           client/s_load.c \
           client/s_main.cpp \
           client/s_mix.cpp \
           client/s_mouth.cpp \
           client/s_stream.cpp \
           client/s_utils.cpp \
           client/s_vox.cpp \
           common/avikit.cpp \
           common/build.cpp \
           common/base_cmd.cpp \
           common/cfgscript.cpp \
           common/cmd.cpp \
           common/common.cpp \
           common/con_utils.cpp \
           common/crclib.cpp \
           common/crtlib.cpp \
           common/cvar.cpp \
           common/filesystem.cpp \
           common/host.cpp \
           common/hpak.cpp \
           common/infostring.cpp \
           common/identification.cpp \
           common/library.cpp \
           common/masterlist.cpp \
           common/mathlib.cpp \
           common/matrixlib.cpp \
           common/mod_studio.cpp \
           common/model.cpp \
           common/net_buffer.cpp \
           common/net_chan.cpp \
           common/net_encode.cpp \
           common/net_huff.cpp \
           common/network.cpp \
           common/pm_surface.cpp \
           common/pm_trace.cpp \
           common/random.cpp \
           common/sys_con.cpp \
           common/system.cpp \
           common/titles.cpp \
           common/world.cpp \
           common/zone.cpp \
           server/sv_client.cpp \
           server/sv_cmds.cpp \
           server/sv_custom.cpp \
           server/sv_frame.cpp \
           server/sv_filter.cpp \
           server/sv_game.cpp \
           server/sv_init.cpp \
           server/sv_main.cpp \
           server/sv_log.cpp \
           server/sv_move.cpp \
           server/sv_phys.cpp \
           server/sv_pmove.cpp \
           server/sv_save.cpp \
           server/sv_world.cpp \
           client/vgui/vgui_draw.cpp \
           client/imagelib/img_bmp.cpp \
           client/imagelib/img_main.c \
           client/imagelib/img_quant.cpp \
           client/imagelib/img_tga.cpp \
           client/imagelib/img_utils.cpp \
           client/imagelib/img_wad.cpp \
           client/imagelib/img_dds.cpp \
           common/soundlib/snd_main.cpp \
           common/soundlib/snd_mp3.cpp \
           common/soundlib/snd_utils.cpp \
           common/soundlib/snd_wav.cpp \
	   common/soundlib/libmpg/dct36.cpp \
	   common/soundlib/libmpg/dct64.cpp \
	   common/soundlib/libmpg/format.cpp \
	   common/soundlib/libmpg/frame.cpp \
	   common/soundlib/libmpg/index.cpp \
	   common/soundlib/libmpg/layer3.cpp \
	   common/soundlib/libmpg/libmpg.cpp \
	   common/soundlib/libmpg/mpg123.cpp \
	   common/soundlib/libmpg/parse.cpp \
	   common/soundlib/libmpg/reader.cpp \
	   common/soundlib/libmpg/synth.cpp \
	   common/soundlib/libmpg/tabinit.cpp \
	   common/sequence.cpp \
           platform/android/vid_android.cpp \
           platform/android/android_nosdl.cpp \
           platform/android/android_fs.cpp \
           platform/posix/crashhandler.c

LOCAL_STATIC_LIBRARIES := NanoGL

LOCAL_LDLIBS := -ldl -llog

include $(BUILD_SHARED_LIBRARY)
