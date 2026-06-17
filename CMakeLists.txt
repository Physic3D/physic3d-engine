#
# Copyright (c) 2015 Pavlo Lavrenenko
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

cmake_minimum_required(VERSION 3.5.1)

# Must be specified before to check if we are targeting 64 bit
option(XASH_64BIT "Enable experimental 64-bit build. Note that you must also provide 64-bit game binaries" OFF) # Enable 64-bit build of Xash3D FWGS Engine.

# Windows XP compatible platform toolset. Must be set before project(),
# otherwise change of CMAKE_*_TOOLSET will take no effect.
if(WIN32 AND NOT XASH_64BIT)
	if ("${CMAKE_GENERATOR}" MATCHES "Visual Studio ([0-9]+)")
		if(${CMAKE_MATCH_1} LESS 11)
			# Nothing. Older VS does support XP by default.
		elseif(${CMAKE_MATCH_1} EQUAL 11)
			set(CMAKE_GENERATOR_TOOLSET "v110_xp" CACHE STRING "CMAKE_GENERATOR_TOOLSET" FORCE)
			set(CMAK_VS_PLATFORM_TOOLSET "v110_xp" CACHE STRING "CMAKE_VS_PLATFORM_TOOLSET" FORCE)
		elseif (${CMAKE_MATCH_1} EQUAL 12)
			set(CMAKE_GENERATOR_TOOLSET "v120_xp" CACHE STRING "CMAKE_GENERATOR_TOOLSET" FORCE)
			set(CMAKE_VS_PLATFORM_TOOLSET "v120_xp" CACHE STRING "CMAKE_VS_PLATFORM_TOOLSET" FORCE)
		elseif (${CMAKE_MATCH_1} EQUAL 14)
			set(CMAKE_GENERATOR_TOOLSET "v140_xp" CACHE STRING "CMAKE_GENERATOR_TOOLSET" FORCE)
			set(CMAKE_VS_PLATFORM_TOOLSET "v140_xp" CACHE STRING "CMAKE_VS_PLATFORM_TOOLSET" FORCE)
		else()
			message(WARNING "WARNING: You maybe building without Windows XP compatibility. See which toolchain version Visual Studio provides, and say cmake to use it: cmake -G \"Visual Studio XX\" -T \"vXXX_xp\"")
		endif()
	endif()
endif()

project(XASH3D)

if(WIN32)
	option(XASH_DOWNLOAD_DEPENDENCIES "Download dependencies automatically, if supported" ON)
else()
	option(XASH_DOWNLOAD_DEPENDENCIES "Download dependencies automatically, if supported" OFF)
endif()

include(${CMAKE_SOURCE_DIR}/cmake_fwgs.cmake)
include(CMakeDependentOption)
include(CPack)

#--------------
# USER DEFINES \
################\

set (XASH3D_VERSION 0.19.4)

option(XASH_DEDICATED "Enable dedicated build. Servers must choose this." OFF) # Servers must choose this

# Windows only. Enable it by default for Windows.
cmake_dependent_option(XASH_NONSTANDARD_LOAD "Enable own DLL loader in Windows." ON "WIN32" OFF)

# POSIX only. Disable it by default for Windows
cmake_dependent_option(XASH_USE_SELECT "Enable reading console commands from stdin." ON "NOT WIN32" OFF)
cmake_dependent_option(XASH_LIBDL "Enable libdl use. POSIX only." ON "NOT WIN32" OFF)

option(XASH_GLES   "Enable if Xash3D is running over GL to GLES translator." OFF)
option(XASH_NANOGL "Use NanoGL(GLES1). Implicitly enables XASH_GLES." OFF)
option(XASH_WES    "Use gl-wes-v2(GLES2). Implicitly enables XASH_GLES." OFF)
fwgs_string_option(XASH_NANOGL_SRC "NanoGL source path" "nanogl")
fwgs_string_option(XASH_WES_SRC "gl-wes-v2 source path" "gl-wes-v2")
option(XASH_SAILFISH "Build for Sailfish OS. Implicitly enables XASH_WES, XASH_GLES." OFF)
option(XASH_STATIC "Static build. Implicitly enables XASH_SINGLE_BINARY." OFF)
option(XASH_NO_ASYNC_NS_RESOLVE "Disable asynchronous domain name resolving." OFF)

option(XASH_VECTORIZE_SINCOS "Try to use vectorized versions of sin(), cos() and sincos()" ON)

# Desktop or dedicated version options
if(XASH_DEDICATED) 
	option(XASH_SDL "Enable SDL." OFF) # Dedicated servers still can use SDL
	option(XASH_SINGLE_BINARY "Don't build game launcher and build engine as executable" ON)
	set(XASH_VGUI OFF)   # But these dedicated can't use at all, so hardcode it as disabled
	set(XASH_MAINUI OFF) # But these dedicated can't use at all, so hardcode it as disabled
else()
	option(XASH_SDL "Enable SDL." ON)
	option(XASH_SINGLE_BINARY "Don't build game launcher and build engine as executable" OFF)
	cmake_dependent_option(XASH_VGUI "Enable VGUI support." ON "NOT XASH_64BIT" OFF)
	option(XASH_MAINUI "Build mainui library" ON) # Totally not recommended to disable
endif()

# Misc
option(XASH_USE_STB_SPRINTF "Use stb_sprintf implementation for sprintf" ON)
option(XASH_NO_INSTALL_RUNSCRIPT "Don't install xash3d.sh" OFF)
option(XASH_NO_INSTALL_VGUI_BIN "Don't bundle proprietary VGUI" OFF)

# Installing
if(WIN32)
	fwgs_string_option(LIB_INSTALL_DIR "Libraries install directory" ${CMAKE_BINARY_DIR})
	fwgs_string_option(LIB_INSTALL_SUBDIR "Libraries install subdirectory" "")
	fwgs_string_option(BIN_INSTALL_DIR "Executables install directory" ${CMAKE_BINARY_DIR})
else()
	fwgs_string_option(LIB_INSTALL_DIR "Libraries install directory" "lib")
	fwgs_string_option(LIB_INSTALL_SUBDIR "Libraries install subdirectory" "xash3d")
	fwgs_string_option(BIN_INSTALL_DIR "Executables install directory" "bin")
endif()


if(NOT XASH_64BIT AND CMAKE_SIZEOF_VOID_P EQUAL 8)
	message(STATUS "64-bit build detected, enabling XASH_64BIT")
	set(XASH_64BIT ON CACHE BOOL "" FORCE)
endif()
if(NOT XASH_LIBDL AND NOT XASH_NONSTANDARD_LOAD)
	message(FATAL_ERROR "You must enable XASH_LIBDL or XASH_NONSTANDARD_LOAD. Otherwise engine is useless.")
endif()

if(XASH_64BIT)
	message(STATUS "Building for 64 Bit")
else()
	message(STATUS "Building for 32 Bit")
endif()


# Dependencies
if(XASH_STATIC)
	set(XASH_SINGLE_BINARY ON)
endif()

if(XASH_SAILFISH)
	set(XASH_WES ON)
endif()

if(XASH_NANOGL OR XASH_WES)
	set(XASH_GLES ON)
endif()

fwgs_conditional_subproject(XASH_MAINUI engine/mainui)
fwgs_conditional_subproject("NOT XASH_64BIT AND XASH_VGUI" engine/vgui_support)
fwgs_conditional_subproject("NOT XASH_SINGLE_BINARY" engine/game_launch)
add_subdirectory(engine)

if(WIN32)
	if(XASH_SINGLE_BINARY)
		set_property(DIRECTORY . PROPERTY VS_STARTUP_PROJECT xash) # Engine project
	else()
		set_property(DIRECTORY . PROPERTY VS_STARTUP_PROJECT xash3d) # Game_launch project
	endif()
endif()
