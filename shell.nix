{ pkgs ? import <nixpkgs> {} }:

pkgs.mkShell {
  name = "physic3d-engine-dev";
  
  buildInputs = with pkgs; [
    # Core build tools
    gcc
    cmake
    gnumake
    pkg-config
    
    # Development libraries for Half-Life/Xash3D engine
    SDL2
    libX11
    libXext
    libGL
    alsa-lib
    zlib
    
    # Additional useful tools
    git
    ccache
    p7zip
  ];
  
  # Environment setup
  shellHook = ''
    echo "======================================"
    echo "Physic3D Engine Development Shell"
    echo "======================================"
    echo "GCC version: $(gcc --version | head -n 1)"
    echo "G++ version: $(g++ --version | head -n 1)"
    echo "CMake version: $(cmake --version | head -n 1)"
    echo "======================================"
    echo "Build commands:"
    echo "  mkdir -p build && cd build"
    echo "  cmake .."
    echo "  make -j\$(nproc)"
    echo "======================================"
  '';
  
  # Set compiler environment variables
  CC = "gcc";
  CXX = "g++";
}
