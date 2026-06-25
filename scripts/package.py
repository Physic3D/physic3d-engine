#!/usr/bin/env python3
"""Cross-platform build & package script for Physic3D."""

import argparse
import os
import platform
import shutil
import subprocess
import sys
import zipfile


def log(msg):
    print(f"  {msg}")


def run(cmd, cwd=None):
    print(f"  $ {cmd}")
    result = subprocess.run(cmd, shell=True, cwd=cwd)
    if result.returncode != 0:
        print(f"ERROR: command failed with exit code {result.returncode}", file=sys.stderr)
        sys.exit(result.returncode)
    return result


def detect_env():
    is_win = platform.system() == "Windows"
    machine = os.environ.get("PROCESSOR_ARCHITECTURE", "") if is_win else platform.machine()

    if is_win:
        default_arch = "x86" if machine.lower() in ("x86", "amd64") else "x64"
    else:
        default_arch = "x64" if "64" in machine else "x86"

    return {"is_win": is_win, "default_arch": default_arch}


def find_file(root, name):
    for dirpath, dirnames, filenames in os.walk(root):
        if name in filenames:
            return os.path.join(dirpath, name)
    return None


def build_engine(src_dir, build_dir, arch, config, is_win):
    log(f"Configuring engine ({arch}, {config})...")

    abs_build = os.path.abspath(build_dir)
    abs_src = os.path.abspath(src_dir)

    cmake_args = [
        f"-S {abs_src}",
        f"-B {abs_build}",
        f"-DCMAKE_BUILD_TYPE={config}",
        "-DXASH_SDL=ON",
        "-DXASH_MAINUI=ON",
        "-DXASH_DEDICATED=OFF",
        "-DXASH_SINGLE_BINARY=OFF",
        "-DXASH_USE_STB_SPRINTF=ON",
        "-DXASH_VECTORIZE_SINCOS=ON",
    ]

    if is_win:
        cmake_args += [
            "-G Ninja",
            f"-A {arch}",
            "-DXASH_VGUI=ON",
            "-DXASH_DOWNLOAD_DEPENDENCIES=ON",
        ]
    else:
        cmake_args += [
            "-G Ninja",
            "-DXASH_VGUI=OFF",
            "-DXASH_DOWNLOAD_DEPENDENCIES=OFF",
        ]

    run(f"cmake {' '.join(cmake_args)}")
    run(f"cmake --build {abs_build} --config {config}")


def build_hlsdk(src_dir, build_dir, arch, config, is_win):
    log(f"Configuring HLSDK ({arch}, {config})...")

    abs_build = os.path.abspath(build_dir)
    abs_src = os.path.abspath(src_dir)

    cmake_args = [
        f"-S {abs_src}",
        f"-B {abs_build}",
        f"-DCMAKE_BUILD_TYPE={config}",
        "-DBUILD_CLIENT=ON",
        "-DBUILD_SERVER=ON",
    ]

    if is_win:
        cmake_args += ["-G Ninja", f"-A {arch}"]
    else:
        cmake_args += ["-G Ninja"]

    run(f"cmake {' '.join(cmake_args)}")
    run(f"cmake --build {abs_build} --config {config}")


def package(engine_build_dir, hlsdk_build_dir, pkg_dir, arch, config, is_win):
    log(f"Packaging into {pkg_dir}...")

    if os.path.exists(pkg_dir):
        shutil.rmtree(pkg_dir)
    os.makedirs(f"{pkg_dir}/valve/cl_dlls", exist_ok=True)
    os.makedirs(f"{pkg_dir}/valve/dlls", exist_ok=True)

    ext_dll = ".dll" if is_win else ".so"
    ext_exe = ".exe" if is_win else ""

    cleanout = os.path.join(os.path.abspath(engine_build_dir), "cleanout")
    if not os.path.isdir(cleanout):
        cleanout = os.path.abspath(engine_build_dir)

    engine_files = {
        f"physic3d_sdl{ext_dll}": f"physic3d_sdl{ext_dll}",
        f"physic3d{ext_exe}": f"physic3d{ext_exe}",
        f"menu{ext_dll}": f"menu{ext_dll}",
    }

    if is_win:
        engine_files["SDL2.dll"] = "SDL2.dll"

    for src_name, dst_name in engine_files.items():
        src = os.path.join(cleanout, src_name)
        if os.path.isfile(src):
            shutil.copy2(src, os.path.join(pkg_dir, dst_name))
            log(f"  {src_name} -> {dst_name}")
        else:
            log(f"  WARNING: {src_name} not found")

    vgui_name = f"vgui_support{ext_dll}"
    vgui_src = os.path.join(cleanout, vgui_name)
    if os.path.isfile(vgui_src):
        shutil.copy2(vgui_src, os.path.join(pkg_dir, vgui_name))
        log(f"  {vgui_name} -> {vgui_name}")

    hlsdk_build = os.path.abspath(hlsdk_build_dir)
    client_src = find_file(hlsdk_build, f"client{ext_dll}")
    server_src = find_file(hlsdk_build, f"hl{ext_dll}")

    if client_src:
        shutil.copy2(client_src, os.path.join(pkg_dir, "valve/cl_dlls", f"client{ext_dll}"))
        log(f"  client{ext_dll} -> valve/cl_dlls/client{ext_dll}")
    else:
        log(f"  WARNING: client{ext_dll} not found")

    if server_src:
        shutil.copy2(server_src, os.path.join(pkg_dir, "valve/dlls", f"hl{ext_dll}"))
        log(f"  hl{ext_dll} -> valve/dlls/hl{ext_dll}")
    else:
        log(f"  WARNING: hl{ext_dll} not found")


def make_archive(pkg_dir, output_path):
    log(f"Creating archive: {output_path}")
    if os.path.exists(output_path):
        os.remove(output_path)

    with zipfile.ZipFile(output_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(pkg_dir):
            for fn in files:
                full = os.path.join(root, fn)
                rel = os.path.relpath(full, os.path.dirname(pkg_dir))
                zf.write(full, rel)
    log(f"  Done: {output_path}")


def main():
    env = detect_env()
    is_win = env["is_win"]

    parser = argparse.ArgumentParser(description="Physic3D build & package script")
    parser.add_argument("--arch", default=env["default_arch"], choices=["x86", "x64"],
                        help=f"Architecture (default: {env['default_arch']})")
    parser.add_argument("--config", default="RelWithDebInfo",
                        choices=["Release", "RelWithDebInfo", "Debug"],
                        help="Build config (default: RelWithDebInfo)")
    parser.add_argument("--src-dir", default=".", help="Source directory")
    parser.add_argument("--build-dir", default="out/build", help="Build output directory")
    parser.add_argument("--dist-dir", default="out/dist", help="Distribution directory")
    parser.add_argument("--no-build-engine", action="store_true", help="Skip engine build")
    parser.add_argument("--no-build-hlsdk", action="store_true", help="Skip HLSDK build")
    parser.add_argument("--no-package", action="store_true", help="Skip packaging")
    args = parser.parse_args()

    src_dir = os.path.abspath(args.src_dir)
    engine_build_dir = os.path.join(args.build_dir, "engine")
    hlsdk_build_dir = os.path.join(args.build_dir, "hlsdk")
    pkg_name = f"physic3d-{args.arch}-{args.config}"
    pkg_dir = os.path.join(os.path.abspath(args.dist_dir), pkg_name)
    archive_path = os.path.join(os.path.abspath(args.dist_dir), f"{pkg_name}.zip")

    print(f"===== Physic3D Build & Package =====")
    print(f"  Platform:  {'Windows' if is_win else 'Linux'}")
    print(f"  Arch:      {args.arch}")
    print(f"  Config:    {args.config}")
    print(f"  Source:    {src_dir}")
    print()

    if not args.no_build_engine:
        build_engine(src_dir, engine_build_dir, args.arch, args.config, is_win)
    if not args.no_build_hlsdk:
        hlsdk_src = os.path.join(src_dir, "engine", "hlsdk")
        build_hlsdk(hlsdk_src, hlsdk_build_dir, args.arch, args.config, is_win)
    if not args.no_package:
        package(engine_build_dir, hlsdk_build_dir, pkg_dir, args.arch, args.config, is_win)
        make_archive(pkg_dir, archive_path)

    print(f"===== Done =====")


if __name__ == "__main__":
    main()
