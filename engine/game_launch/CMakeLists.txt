#
# Copyright (c) 2015 Pavlo Lavrenenko, Alibek Omarov
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

cmake_minimum_required(VERSION 3.5.1...3.28)
project(XASH_LAUNCHER CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(XASH_LAUNCHER physic3d_launcher)
fwgs_fix_default_msvc_settings()
file(GLOB XASH_LAUNCHER_SOURCES *.cpp)
if(WIN32)
	list(APPEND XASH_LAUNCHER_SOURCES game.rc)
endif()

add_executable(${XASH_LAUNCHER} WIN32 ${XASH_LAUNCHER_SOURCES})

# SDL2 provides it's own crossplatform main() implementation and simple message boxes
# Win32 uses WinMain() implemented in launcher code and system message boxes
# Otherwise it prints errors in stdin.
if(XASH_SDL AND NOT WIN32)
	set(SDL2_BUILDING_EXECUTABLE YES) # Link SDL2main also
	xash_link_sdl2(${XASH_LAUNCHER})
	add_definitions(-DXASH_SDL)
endif()

if(MSVC)
        add_definitions(-D_CRT_SECURE_NO_WARNINGS)
endif()

set_target_properties(${XASH_LAUNCHER} PROPERTIES
	OUTPUT_NAME physic3d)

target_link_libraries(${XASH_LAUNCHER} ${CMAKE_DL_LIBS})

fwgs_install(${XASH_LAUNCHER})
