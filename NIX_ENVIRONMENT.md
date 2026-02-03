# Nix Development Environment

This repository includes Nix configuration files for a reproducible development environment for building the Physic3D Engine (Xash3D Half-Life Engine).

## Prerequisites

You need to have Nix installed on your system:
- **Linux/macOS**: Follow the installation guide at https://nixos.org/download.html
- **Quick install**: `curl -L https://nixos.org/nix/install | sh`

## Using the Development Shell

### Option 1: Classic Nix Shell (shell.nix)

```bash
nix-shell
```

This will drop you into a shell with all the necessary development tools including:
- GCC and G++
- CMake
- SDL2, OpenGL, ALSA, and other required libraries
- Build tools (make, pkg-config, git, ccache)

### Option 2: Nix Flakes (flake.nix)

If you have Nix Flakes enabled:

```bash
nix develop
```

Or for a one-time command:

```bash
nix develop --command bash
```

To enable Nix Flakes, add to `~/.config/nix/nix.conf`:
```
experimental-features = nix-command flakes
```

## Building the Engine

Once in the Nix shell:

```bash
# Create build directory
mkdir -p build && cd build

# Configure with CMake
cmake ..

# Build
make -j$(nproc)
```

### Build Options

For dedicated server build:
```bash
cmake -DXASH_DEDICATED=ON -DXASH_SDL=OFF ..
make
```

For desktop build with VGUI:
```bash
cmake -DXASH_VGUI=ON -DMAINUI_USE_STB=ON ..
make
```

## Environment Variables

The Nix shell automatically sets:
- `CC=gcc` - C compiler
- `CXX=g++` - C++ compiler

## Updating Dependencies

To update the Nix packages to the latest versions:

**For shell.nix:**
```bash
nix-channel --update
```

**For flake.nix:**
```bash
nix flake update
```

## Troubleshooting

### "nix-shell: command not found"
Make sure Nix is properly installed and in your PATH. You may need to source the Nix profile:
```bash
source ~/.nix-profile/etc/profile.d/nix.sh
```

### Missing libraries
The shell.nix includes all common dependencies. If you encounter missing libraries, please open an issue or update the `buildInputs` list in shell.nix.

## Additional Resources

- [Nix Manual](https://nixos.org/manual/nix/stable/)
- [Xash3D FWGS Documentation](https://github.com/FWGS/xash3d-fwgs)
- [Physic3D Engine Repository](https://github.com/bariscodefxy/physic3d-engine)
