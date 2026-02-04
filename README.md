### Physic3D Engine
*Xash3D but we added some customizations. (Old Engine)*

![GitHub Repo stars](https://img.shields.io/github/stars/Physic3d/physic3d-engine)
![Gitlab Pipeline Status](https://img.shields.io/gitlab/pipeline-status/Physic3d%2Fengine%2Fphysic3d-engine?gitlab_url=https%3A%2F%2Fgit.bariscodefx.tr&branch=master)
[![Discord](https://img.shields.io/badge/Discord-Join-7289da?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/x7dn4JMCnK)

### [ Developers ]

- [iQuitt](https://github.com/iquitt)
- [byoreo](https://github.com/byoreo)
- [hasandramali](https://github.com/hasandramali)
- [bariscodefx](https://github.com/bariscodefxy)

### [ Features ]

Physic3D Engine is a customized fork of Xash3D with the following enhancements:

#### 🎯 **Visual Enhancements**
- **Custom Crosshair System** - Fully customizable crosshair with color, size, gap, thickness, and dynamic movement options
- **Viewmodel Effects** - Viewmodel lag with multiple styles, scale, and speed controls
- **Glow Effects** - Player, viewmodel, and worldmodel glow with customizable colors, render modes, and intensity
- **Thirdperson Camera** - Adjustable thirdperson view with forward, right, and up offset controls
- **Fog Support** - Enhanced fog rendering for atmospheric effects

#### 🎮 **Gameplay Features**
- **Weapon Inspect** - Inspect your weapon with `do_inspect` command
- **Team-based Glow** - Glow effects that respect team colors (added Oct 2024)

#### 🌐 **Network & Server**
- **TSource Query Support** - Improved server query system using TSource protocol (added Aug 2024)
- **Fast Reconnect** - Retry command and fast reconnect functionality for better connection handling (added Aug 2024)
- **Android Server Support** - Enhanced server response for Android clients

#### 🛠️ **Development & Build**
- **Nix Development Environment** - Reproducible development environment with all dependencies (added Feb 2024)
- **C11 Standard Support** - Updated to C11 standard for better compatibility
- **Root User Warnings** - Safety warnings when running as root user (added Dec 2024)

#### 📱 **Platform Support**
- **Android Build** - Full Android support with dedicated builds

### [ Development Environment ]

**Nix Development Shell** - For a reproducible development environment with GCC, G++, and all dependencies:

```bash
# Using classic Nix
nix-shell

# Or using Nix Flakes
nix develop
```

See [NIX_ENVIRONMENT.md](NIX_ENVIRONMENT.md) for detailed instructions.

### [ Some Commands ]
```py
// Custom Crosshair //

xhair_enable 
xhair_alpha
xhair_color_r
xhair_color_g
xhair_color_b
xhair_dot
xhair_dynamic_move
xhair_dynamic_scale
xhair_gap_useweaponvalue
xhair_gap 
xhair_pad
xhair_size
xhair_t
xhair_thick

// Viewmodel //

viewmodel_lag_style (1/2)
viewmodel_lag_scale
viewmodel_lag_speed
do_inspect

// Player Glow //

cl_glow_player
cl_glow_player_red
cl_glow_player_blue
cl_glow_player_green
cl_glow_player_rendermode
cl_glow_player_renderamt

// Viewmodel Glow //

cl_glow_viewmodel
cl_glow_viewmodel_red
cl_glow_viewmodel_blue
cl_glow_viewmodel_green
cl_glow_viewmodel_renderamt

// Worldmodel Glow //

cl_glow_worldmodel (1/2)
cl_glow_worldmodel_red
cl_glow_worldmodel_green
cl_glow_worldmodel_blue
cl_glow_worldmodel_renderamt
cl_glow_worldmodel_height
cl_glow_worldmodel_spin

// Thirdperson //

cl_thirdperson
cl_thirdperson_forward
cl_thirdperson_right
cl_thirdperson_up

```
