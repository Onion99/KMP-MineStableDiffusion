# 更新日志

本项目的所有重要更改都将记录在此文件中。

本项目遵循 [语义化版本](https://semver.org/spec/v2.0.0.html)。

## v5.1.0

- Fix: com/sun/security/auth/module/UnixSystem error on Linux

## v5.0.0

- IOS support

## v4.4.0

- fix linux sdloader error

## v4.3.0

- translation modify

## v4.2.1

- android enable mmap default

## v4.2.0

- modify: offloadToCpu && keepClipOnCpu && keepVaeOnCpu 

## v4.1.0

- fix: linux CXXABL1.3.13'not found problem

## v4.0.0

- fix linux system error:The following packages have unmet dependencies
- fix android ui visible problem and exception

## v3.2.0

- add clipLPath、clipGPath、t5xxlPath

## v3.1.0

- Fix FLUX Model loading issue

## v3.0.0

### 🎨 Advanced Settings Overhaul

#### New Features
- **CPU Offloading Controls**: Added three independent toggles for fine-grained memory management
  - `offloadToCpu`: Offload model computations to CPU to save GPU/memory
  - `keepClipOnCpu`: Keep CLIP model on CPU to reduce GPU memory usage
  - `keepVaeOnCpu`: Keep VAE decoder on CPU to reduce GPU memory usage
  - All parameters now user-controllable in Advanced Settings (previously hardcoded)

- **Expanded Quantization Options**: Increased from 6 to 13 quantization types
  - Added K-series variants: `Q2_K`, `Q3_K`, `Q4_K`, `Q5_K`, `Q6_K` (better quality at same bit-depth)
  - Added `BF16` (Brain Float 16) for modern AI hardware optimization
  - New "Auto" mode (default) that lets the library select optimal quantization
  - Full list: Auto → F32 → F16 → BF16 → Q8_0 → Q6_K → Q5_K → Q5_0 → Q4_K → Q4_0 → Q3_K → Q2_K

#### UI/UX Improvements
- **Enhanced Flash Attention Documentation**
  - Detailed benefits info box showing 30-50% memory reduction
  - Compatibility notes for NVIDIA GPUs (compute capability ≥7.5)
  - Clear recommendations: Enable for devices with <8GB RAM
  - Dynamic info box (shows only when enabled)

- **Comprehensive Quantization Guide**
  - Warning box alerting users about re-initialization requirements
  - Detailed info box with memory usage and quality tradeoffs for all 13 types
  - Example: "Q4_K: 4-bit K-variant, ~1GB, better than Q4_0"
  - Note explaining K-variants offer superior quality at same bit-depth

- **Visual Design Enhancements**
  - Color-coded info boxes:
    - 🟧 Orange warning box for quantization risks
    - 🔵 Blue info box for technical details
    - 🟢 Green success box for Flash Attention benefits
  - Monospace font for technical specifications
  - Improved readability with proper line spacing

#### Technical Changes
- **Default Value Changes**
  - `wtype` default changed from `0` (F32) to `-1` (Auto)
  - C++ JNI layer now skips `wtype` assignment when value is `-1`
  - Allows underlying stable-diffusion.cpp library to use its optimal defaults

- **Internationalization**
  - Added 15+ new string resources (English + Chinese)
  - All new settings fully localized

#### Developer Notes
- Updated `DiffusionLoader` interface across all platforms (common, Android, Desktop)
- Modified `ChatViewModel` to include new mutable state properties
- Enhanced `AdvancedSettingScreen.kt` with conditional rendering and info boxes
- C++ native code (`diffusion_loader_jni.cpp`) updated to handle new parameters

---



## v2.3.0

- Flash Attention Switch
- Quantization Type

## v2.2.0

- add image to save

## v2.1.0

- Android file selection optimization

## v2.0.0

- support z_image_turbo model

## v1.3.2

- ggml and sd backend use METAL in


## v1.3.1

- fix dylib copy in MacOS


## v1.3.0

- jvmArgs change

## v1.2.0

- adapt to android soft keyboard input
- fix android 16kb size
- android app icon add

## v1.1.0

- Custom generated parameters

## v1.0.0


- MineStableDiffusion First Release
- Core:
    - Multiplatform (Android,DeskTop)
    - txt2Img
    - MultiLanguage