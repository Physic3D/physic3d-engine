#
# Copyright (c) 2018 a1batross
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

option(FWGSLIB_DEBUG "Print debug messages for FWGSLib" OFF)
option(XASH_FORCE_PIE "Force enable -fpie/-fPIC" OFF)

macro(fwgs_debug)
	if(FWGSLIB_DEBUG)
		message(${ARGV})
	endif()
endmacro()

macro(fwgs_install tgt)
	if(NOT MSVC)
		install(TARGETS ${tgt} DESTINATION ${LIB_INSTALL_DIR}/${LIB_INSTALL_SUBDIR}
			PERMISSIONS OWNER_READ OWNER_WRITE OWNER_EXECUTE
			    GROUP_READ GROUP_EXECUTE
				WORLD_READ WORLD_EXECUTE)
	else()
		install(TARGETS ${tgt}
			CONFIGURATIONS Debug
			RUNTIME DESTINATION ${LIB_INSTALL_DIR}/Debug/
			LIBRARY DESTINATION ${LIB_INSTALL_DIR}/Debug/)
		install(FILES $<TARGET_PDB_FILE:${tgt}>
			CONFIGURATIONS Debug
			DESTINATION ${LIB_INSTALL_DIR}/Debug/ )
		install(TARGETS ${tgt}
			CONFIGURATIONS Release
			RUNTIME DESTINATION ${LIB_INSTALL_DIR}/Release/
			LIBRARY DESTINATION ${LIB_INSTALL_DIR}/Release/)
	endif()
endmacro()

macro(fwgs_conditional_subproject cond subproject)
	set(TEMP 1)
	foreach(d ${cond})
		string(REGEX REPLACE " +" ";" expr "${d}")
		if(${expr})
		else()
			set(TEMP 0)
		endif()
    endforeach()
	if(TEMP)
		add_subdirectory(${subproject})
	endif()
endmacro()

macro(fwgs_set_default_properties tgt)
	if( XASH_FORCE_PIE OR (APPLE AND CMAKE_OSX_ARCHITECTURES) OR NOT (ARCH STREQUAL "i386"))
		message(STATUS "Enabled PIE for ${tgt}")
		set_target_properties(${tgt} PROPERTIES
			POSITION_INDEPENDENT_CODE 1)
	else()
		message(STATUS "Disabled PIE for ${tgt}")
	endif()
	if(WIN32)
		set_target_properties(${tgt} PROPERTIES
			PREFIX "")
	endif()
endmacro()

macro(fwgs_string_option name description value)
	set(${name} ${value} CACHE STRING ${description})
endmacro()

macro(cond_list_append arg1 arg2 arg3)
	set(TEMP 1)
	foreach(d ${arg1})
		string(REGEX REPLACE " +" ";" expr "${d}")
		if(${expr})
		else()
			set(TEMP 0)
		endif()
    endforeach()
	if(TEMP)
		list(APPEND ${arg2} ${${arg3}})
	endif()
endmacro()

macro(fwgs_unpack_file file path)
	message(STATUS "Unpacking ${file} to ${CMAKE_BINARY_DIR}/${path}")
	execute_process(COMMAND ${CMAKE_COMMAND} -E make_directory ${CMAKE_BINARY_DIR}/${path})
	execute_process(COMMAND ${CMAKE_COMMAND} -E tar xzf ${file}
		WORKING_DIRECTORY ${CMAKE_BINARY_DIR}/${path})
endmacro()

macro(target_link_vgui_hack arg1)
	if(WIN32)
		target_link_libraries(${arg1} ${VGUI_LIBRARY} )
	elseif (${CMAKE_SYSTEM_NAME} MATCHES "Darwin")
		target_link_libraries(${arg1} ${VGUI_LIBRARY} )
	elseif (${CMAKE_SYSTEM_NAME} MATCHES "Linux")
		add_custom_command(TARGET ${arg1} PRE_LINK COMMAND
			${CMAKE_COMMAND} -E copy ${VGUI_LIBRARY} $<TARGET_FILE_DIR:${arg1}>)
		target_link_libraries(${arg1} ${VGUI_LIBRARY})
	endif()
endmacro()

function(_fwgs_find_sdl2_after_download)
	find_path(SDL2_INCLUDE_DIR SDL.h
		PATHS ${SDL2_PATH}
		PATH_SUFFIXES include/SDL2 include
		NO_DEFAULT_PATH)
	if(MSVC)
		if(XASH_64BIT)
			set(_lib_suffix lib/x64)
		else()
			set(_lib_suffix lib/x86)
		endif()
	elseif(MINGW)
		set(_lib_suffix lib)
	endif()
	find_library(SDL2_LIBRARY NAMES SDL2 SDL2.dll
		PATHS ${SDL2_PATH}
		PATH_SUFFIXES ${_lib_suffix}
		NO_DEFAULT_PATH)
	if(SDL2_INCLUDE_DIR AND SDL2_LIBRARY)
		set(SDL2_FOUND TRUE CACHE BOOL "" FORCE)
	endif()
	set(SDL2_INCLUDE_DIR ${SDL2_INCLUDE_DIR} CACHE STRING "" FORCE)
	set(SDL2_LIBRARY ${SDL2_LIBRARY} CACHE STRING "" FORCE)
endfunction()

function(_fwgs_find_vgui_after_download)
	find_path(VGUI_INCLUDE_DIR VGUI.h
		PATHS ${HL_SDK_DIR}
		PATH_SUFFIXES utils/vgui/include include
		NO_DEFAULT_PATH)
	find_library(VGUI_LIBRARY NAMES vgui vgui.so
		PATHS ${HL_SDK_DIR}
		PATH_SUFFIXES utils/vgui/lib/win32_vc6 lib/win32_vc6 lib
		NO_DEFAULT_PATH)
	if(VGUI_INCLUDE_DIR AND VGUI_LIBRARY)
		set(VGUI_FOUND TRUE CACHE BOOL "" FORCE)
	endif()
	set(VGUI_INCLUDE_DIR ${VGUI_INCLUDE_DIR} CACHE STRING "" FORCE)
	set(VGUI_LIBRARY ${VGUI_LIBRARY} CACHE STRING "" FORCE)
endfunction()

macro(fwgs_library_dependency tgt pkgname)
	if(XASH_DOWNLOAD_DEPENDENCIES AND ${ARGC} GREATER 2)
		set(FORCE_DOWNLOAD FALSE)
		set(FORCE_UNPACK FALSE)
		if(NOT ${${pkgname}_FOUND})
			set(FORCE_DOWNLOAD TRUE)
			set(FORCE_UNPACK TRUE)
		else()
			if(NOT EXISTS "${${pkgname}_LIBRARY}")
				if(NOT EXISTS "${CMAKE_BINARY_DIR}/${pkgname}")
					set(FORCE_UNPACK TRUE)
					if(NOT EXISTS "${CMAKE_BINARY_DIR}/${ARGV3}")
						set(FORCE_DOWNLOAD TRUE)
					endif()
				endif()
			endif()
		endif()
		if(FORCE_DOWNLOAD)
			message(STATUS "Downloading ${pkgname} dependency for ${tgt} from ${ARGV2} to ${ARGV3}")
			file(DOWNLOAD "${ARGV2}" "${CMAKE_BINARY_DIR}/${ARGV3}")
			set(FORCE_UNPACK TRUE)
		endif()
		if(FORCE_UNPACK)
			fwgs_unpack_file("${CMAKE_BINARY_DIR}/${ARGV3}" "${pkgname}")
		endif()
		if(FORCE_DOWNLOAD OR FORCE_UNPACK)
			set(${ARGV4} ${CMAKE_BINARY_DIR}/${pkgname}/${ARGV5})
			if(${pkgname} STREQUAL "SDL2")
				_fwgs_find_sdl2_after_download()
			elseif(${pkgname} STREQUAL "VGUI")
				_fwgs_find_vgui_after_download()
			else()
				find_package(${pkgname} REQUIRED)
			endif()
		endif()
	else()
		if(${pkgname} STREQUAL "VGUI")
			_fwgs_find_vgui()
			if(NOT VGUI_FOUND)
				message(FATAL_ERROR "VGUI not found. Set HL_SDK_DIR or enable XASH_DOWNLOAD_DEPENDENCIES.")
			endif()
		elseif(${pkgname} STREQUAL "SDL2")
			if(NOT SDL2_FOUND)
				message(FATAL_ERROR "SDL2 not found. Set SDL2_PATH or install SDL2 development package.")
			endif()
		else()
			find_package(${pkgname} REQUIRED)
		endif()
	endif()
	include_directories(${${pkgname}_INCLUDE_DIR})
	if(${pkgname} STREQUAL VGUI)
		target_link_vgui_hack(${tgt})
	else()
		target_link_libraries(${tgt} ${${pkgname}_LIBRARY})
	endif()
endmacro()

macro(fwgs_add_compile_options lang)
	set(FLAGS ${ARGV})
	list(REMOVE_AT FLAGS 0)
	string(REPLACE ";" " " FLAGS_STR "${FLAGS}")
	if(${lang} STREQUAL "C")
		fwgs_debug(STATUS "Adding ${FLAGS_STR} for both C/CXX")
		set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${FLAGS_STR}")
		set(CMAKE_CXX_FLAGS "${CMAKE_C_FLAGS} ${FLAGS_STR}")
	elseif(${lang} STREQUAL "CONLY")
		fwgs_debug(STATUS "Adding ${FLAGS_STR} for C ONLY")
		set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${FLAGS_STR}")
	elseif(${lang} STREQUAL "CXX")
		fwgs_debug(STATUS "Adding ${FLAGS_STR} for CXX")
		set(CMAKE_CXX_FLAGS "${CMAKE_C_FLAGS} ${FLAGS_STR}")
	endif()
endmacro()

macro(fwgs_fix_default_msvc_settings)
	if (MSVC)
		foreach (flag_var
			CMAKE_C_FLAGS CMAKE_C_FLAGS_DEBUG CMAKE_C_FLAGS_RELEASE
			CMAKE_C_FLAGS_MINSIZEREL CMAKE_C_FLAGS_RELWITHDEBINFO
        		CMAKE_CXX_FLAGS CMAKE_CXX_FLAGS_DEBUG CMAKE_CXX_FLAGS_RELEASE
			CMAKE_CXX_FLAGS_MINSIZEREL CMAKE_CXX_FLAGS_RELWITHDEBINFO)
			string(REPLACE "/MD" "/MT" ${flag_var} "${${flag_var}}")
		endforeach()
		# /MP is a compiler flag for cl.exe only; keep it out of resource compilation.
		set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} /MP")
		set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /MP")
	endif()
endmacro()

macro(xash_link_sdl2 tgt)
        if(WIN32)
                set(SDL2_VER "2.0.14")
                set(SDL2_DEVELPKG "VC.zip")
                set(SDL2_SUBDIR "SDL2-${SDL2_VER}")
                set(SDL2_ARCHIVE "SDL2.zip")
                if(MINGW)
                        if(XASH_64BIT)
                                set(SDL2_SUBDIR "SDL2-${SDL2_VER}/x86_64-w64-mingw32")
                        else()
                                set(SDL2_SUBDIR "SDL2-${SDL2_VER}/i686-w64-mingw32")
                        endif()
                        set(SDL2_DEVELPKG "mingw.tar.gz")
                        set(SDL2_ARCHIVE "SDL2.tar.gz")
                endif()
                set(SDL2_DOWNLOAD_URL "http://libsdl.org/release/SDL2-devel-${SDL2_VER}-${SDL2_DEVELPKG}")
                fwgs_library_dependency(${tgt} SDL2 ${SDL2_DOWNLOAD_URL} ${SDL2_ARCHIVE} SDL2_PATH ${SDL2_SUBDIR})
        else()
                fwgs_library_dependency(${tgt} SDL2)
        endif()
endmacro()

set(archdetect_c_code "
#if defined(__arm__) || defined(__TARGET_ARCH_ARM)
    #if defined(__ARM_ARCH_7__) \\
	    || defined(__ARM_ARCH_7A__) \\
		|| defined(__ARM_ARCH_7R__) \\
		|| defined(__ARM_ARCH_7M__) \\
		|| (defined(__TARGET_ARCH_ARM) && __TARGET_ARCH_ARM-0 >= 7)
		#error cmake_ARCH armv7
		#elif defined(__ARM_ARCH_6__) \\
		|| defined(__ARM_ARCH_6J__) \\
		|| defined(__ARM_ARCH_6T2__) \\
		|| defined(__ARM_ARCH_6Z__) \\
		|| defined(__ARM_ARCH_6K__) \\
		|| defined(__ARM_ARCH_6ZK__) \\
		|| defined(__ARM_ARCH_6M__) \\
		|| (defined(__TARGET_ARCH_ARM) && __TARGET_ARCH_ARM-0 >= 6)
		#error cmake_ARCH armv6
		#elif defined(__ARM_ARCH_5TEJ__) \\
		|| (defined(__TARGET_ARCH_ARM) && __TARGET_ARCH_ARM-0 >= 5)
		#error cmake_ARCH armv5
		#else
		#error cmake_ARCH arm
		#endif
#elif defined(__i386) || defined(__i386__) || defined(_M_IX86)
    #error cmake_ARCH i386
#elif defined(__x86_64) || defined(__x86_64__) || defined(__amd64) || defined(_M_X64)
    #error cmake_ARCH x86_64
#elif defined(__ia64) || defined(__ia64__) || defined(_M_IA64)
    #error cmake_ARCH ia64
#elif defined(__ppc__) || defined(__ppc) || defined(__powerpc__) \\
      || defined(_ARCH_COM) || defined(_ARCH_PWR) || defined(_ARCH_PPC)  \\
	  || defined(_M_MPPC) || defined(_M_PPC)
	#if defined(__ppc64__) || defined(__powerpc64__) || defined(__64BIT__)
	    #error cmake_ARCH ppc64
		#else
		#error cmake_ARCH ppc
		#endif
#endif

#error cmake_ARCH unknown
")

function(target_architecture output_var)
	if(APPLE AND CMAKE_OSX_ARCHITECTURES)
		foreach(osx_arch ${CMAKE_OSX_ARCHITECTURES})
			if("${osx_arch}" STREQUAL "ppc" AND ppc_support)
				set(osx_arch_ppc TRUE)
			elseif("${osx_arch}" STREQUAL "i386")
				set(osx_arch_i386 TRUE)
			elseif("${osx_arch}" STREQUAL "x86_64")
				set(osx_arch_x86_64 TRUE)
			elseif("${osx_arch}" STREQUAL "ppc64" AND ppc_support)
				set(osx_arch_ppc64 TRUE)
			else()
				message(FATAL_ERROR "Invalid OS X arch name: ${osx_arch}")
			endif()
		endforeach()
		if(osx_arch_ppc)
			list(APPEND ARCH ppc)
		endif()
		if(osx_arch_i386)
			list(APPEND ARCH i386)
		endif()
		if(osx_arch_x86_64)
			list(APPEND ARCH x86_64)
		endif()
		if(osx_arch_ppc64)
			list(APPEND ARCH ppc64)
		endif()
	else()
		file(WRITE "${CMAKE_BINARY_DIR}/arch.c" "${archdetect_c_code}")
		enable_language(C)
		try_run(
			run_result_unused
			compile_result_unused
			"${CMAKE_BINARY_DIR}"
			"${CMAKE_BINARY_DIR}/arch.c"
			COMPILE_OUTPUT_VARIABLE ARCH
			CMAKE_FLAGS CMAKE_OSX_ARCHITECTURES=${CMAKE_OSX_ARCHITECTURES}
			)
		string(REGEX MATCH "cmake_ARCH ([a-zA-Z0-9_]+)" ARCH "${ARCH}")
		string(REPLACE "cmake_ARCH " "" ARCH "${ARCH}")
		if (NOT ARCH)
			set(ARCH unknown)
		endif()
	endif()
	message(STATUS "Target architecture: ${ARCH}")
	set(${output_var} "${ARCH}" PARENT_SCOPE)
endfunction()

target_architecture(ARCH)

# FindLibunwind -- inlined Find module
if(NOT LIBUNWIND_FOUND)
	find_path(LIBUNWIND_INCLUDE_DIR libunwind.h
	  /usr/include
	  /usr/local/include
	)
	find_library(LIBUNWIND_LIBRARIES NAMES unwind )
	if(NOT LIBUNWIND_LIBRARIES STREQUAL "LIBUNWIND_LIBRARIES-NOTFOUND")
	  if (CMAKE_COMPILER_IS_GNUCC)
	    set(LIBUNWIND_LIBRARIES "gcc_eh;${LIBUNWIND_LIBRARIES}")
	  endif()
	endif()
	message(STATUS "looking for liblzma")
	find_library(LIBLZMA_LIBRARIES lzma )
	if(NOT LIBLZMA_LIBRARIES STREQUAL "LIBLZMA_LIBRARIES-NOTFOUND")
	  message(STATUS "liblzma found")
	  set(LIBUNWIND_LIBRARIES "${LIBUNWIND_LIBRARIES};${LIBLZMA_LIBRARIES}")
	endif()
	include(FindPackageHandleStandardArgs)
	find_package_handle_standard_args(Libunwind "Could not find libunwind" LIBUNWIND_INCLUDE_DIR LIBUNWIND_LIBRARIES)
	mark_as_advanced(LIBUNWIND_INCLUDE_DIR LIBUNWIND_LIBRARIES)
endif()

# FindSDL2 -- inlined Find module
if(NOT DEFINED XASH_SDL OR XASH_SDL)
if(WIN32 AND (NOT SDL2_PATH AND NOT XASH_DOWNLOAD_DEPENDENCIES))
	message(FATAL_ERROR "To find SDL2 correctly, you need to pass SDL2_PATH variable to CMake")
endif()
endif()

if(NOT SDL2_FOUND AND (NOT DEFINED XASH_SDL OR XASH_SDL))
	set(SDL2_SEARCH_PATHS
		${SDL2_PATH}
		${CMAKE_LIBRARY_PATH}
		~/Library/Frameworks
		/Library/Frameworks
		/usr/local
		/usr
	)

	find_path(SDL2_INCLUDE_DIR SDL.h
		PATH_SUFFIXES include/SDL2 include
		PATHS ${SDL2_SEARCH_PATHS}
	)

	if(XASH_64BIT)
		find_library(SDL2_LIBRARY_TEMP
		NAMES SDL2 SDL2.dll
		PATH_SUFFIXES
		    lib
			lib/x86_64-linux-gnu
			lib/x64
		PATHS ${SDL2_SEARCH_PATHS}
		)
	else()
	find_library(SDL2_LIBRARY_TEMP
		NAMES SDL2 SDL2.dll
		PATH_SUFFIXES
		    lib
			lib/i386-linux-gnu
			lib/x86
		PATHS ${SDL2_SEARCH_PATHS}
	)
	endif()

	if(NOT SDL2_LIBRARY_TEMP AND NOT WIN32)
		execute_process(COMMAND sdl2-config --libs
			RESULT_VARIABLE SDL2_CONFIG_RETVAL
			OUTPUT_VARIABLE SDL2_LIBRARY_TEMP
			OUTPUT_STRIP_TRAILING_WHITESPACE)
		if(SDL2_CONFIG_RETVAL)
			message(SEND_ERROR "sdl2-config --libs returned code ${SDL2_CONFIG_RETVAL}. Check that sdl2-config is working.")
		endif()
	endif()

	if(SDL2_BUILDING_EXECUTABLE)
		if(NOT ${SDL2_INCLUDE_DIR} MATCHES ".framework")
			if(XASH_64BIT)
				find_library(SDL2MAIN_LIBRARY
					NAMES SDL2main libSDL2main.a
					PATH_SUFFIXES
						lib
						lib/x86_64-linux-gnu
						lib/x64
					PATHS ${SDL2_SEARCH_PATHS})
			else()
				find_library(SDL2MAIN_LIBRARY
					NAMES SDL2main libSDL2main.a
					PATH_SUFFIXES
						lib
						lib/i386-linux-gnu
						lib/x86
					PATHS ${SDL2_SEARCH_PATHS})
			endif()
		endif()
	endif()

	if(NOT APPLE)
		find_package(Threads)
	endif()

	if(MINGW)
		set(MINGW32_LIBRARY mingw32 CACHE STRING "mwindows for MinGW")
	endif()

	if(SDL2_LIBRARY_TEMP)
		if(SDL2_BUILDING_EXECUTABLE AND SDL2MAIN_LIBRARY)
			set(SDL2_LIBRARY_TEMP ${SDL2MAIN_LIBRARY} ${SDL2_LIBRARY_TEMP})
		endif()
		if(APPLE)
			set(SDL2_LIBRARY_TEMP ${SDL2_LIBRARY_TEMP} "-framework Cocoa")
		endif()
		if(NOT APPLE)
			set(SDL2_LIBRARY_TEMP ${SDL2_LIBRARY_TEMP} ${CMAKE_THREAD_LIBS_INIT})
		endif()
		if(MINGW)
			set(SDL2_LIBRARY_TEMP ${MINGW32_LIBRARY} ${SDL2_LIBRARY_TEMP})
		endif()
		set(SDL2_LIBRARY ${SDL2_LIBRARY_TEMP} CACHE STRING "Where the SDL2 Library can be found")
		set(SDL2_LIBRARY_TEMP "${SDL2_LIBRARY_TEMP}" CACHE INTERNAL "")
	endif()

	include(FindPackageHandleStandardArgs)
	find_package_handle_standard_args(SDL2 REQUIRED_VARS SDL2_LIBRARY SDL2_INCLUDE_DIR)
endif()

# FindVGUI -- inlined Find module (lazy, called only from fwgs_library_dependency)
function(_fwgs_find_vgui)
	if(NOT HL_SDK_DIR AND NOT XASH_DOWNLOAD_DEPENDENCIES)
		message(FATAL_ERROR "Pass a HL_SDK_DIR variable to CMake to be able use VGUI")
	endif()
	if(NOT VGUI_FOUND)
		set(VGUI_SEARCH_PATHS ${HL_SDK_DIR})
		find_path(VGUI_INCLUDE_DIR
			VGUI.h
			HINTS $ENV{VGUIDIR}
			PATH_SUFFIXES
			    utils/vgui/include/
				include/
			PATHS ${VGUI_SEARCH_PATHS}
		)
		if(APPLE)
			set(LIBNAMES vgui.dylib)
		else()
			set(LIBNAMES vgui vgui.so)
		endif()
		find_library(VGUI_LIBRARY
			NAMES ${LIBNAMES}
			HINTS $ENV{VGUIDIR}
			PATH_SUFFIXES
				games/lib/xash3d
				lib/xash3d
				xash3d/
				utils/vgui/lib/win32_vc6
				linux/
				linux/release
				lib/win32_vc6
				lib/
			PATHS ${VGUI_SEARCH_PATHS}
		)
		include(FindPackageHandleStandardArgs)
		find_package_handle_standard_args(VGUI REQUIRED_VARS VGUI_LIBRARY VGUI_INCLUDE_DIR)
	endif()
endfunction()
