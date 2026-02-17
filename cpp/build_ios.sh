#!/bin/bash
set -e

# Go to cpp directory
cd "$(dirname "$0")"

# One-time clean to avoid stale files
# rm -rf build-ios-device build-ios-sim-arm64 build-ios-sim-x64 libs/ios-device libs/ios-simulator

# ==========================================
# 1. Build for iOS Device (iphoneos)
# ==========================================
echo "Building for iOS Device (iphoneos, arm64)..."
cmake -S stable-diffusion.cpp -B build-ios-device -G Xcode \
    -DCMAKE_SYSTEM_NAME=iOS \
    -DCMAKE_OSX_SYSROOT=iphoneos \
    -DCMAKE_OSX_DEPLOYMENT_TARGET=14.0 \
    -DCMAKE_OSX_ARCHITECTURES="arm64" \
    -DSD_METAL=ON \
    -DSD_BUILD_SHARED_LIBS=OFF \
    -DSD_BUILD_EXAMPLES=OFF \
    -DCMAKE_C_FLAGS="-w" \
    -DCMAKE_CXX_FLAGS="-w"

cmake --build build-ios-device --config Release --target stable-diffusion

mkdir -p libs/ios-device
find build-ios-device -name "libstable-diffusion.a" -exec cp {} libs/ios-device/ \;
find build-ios-device -name "libggml*.a" -exec cp {} libs/ios-device/ \;

# ==========================================
# 2. Build for iOS Simulator (iphonesimulator)
# ==========================================
echo "Building for iOS Simulator..."
mkdir -p libs/ios-simulator

# 2a. Simulator arm64 (Apple Silicon)
echo "  - Building Simulator arm64..."
cmake -S stable-diffusion.cpp -B build-ios-sim-arm64 -G Xcode \
    -DCMAKE_SYSTEM_NAME=iOS \
    -DCMAKE_OSX_SYSROOT=iphonesimulator \
    -DCMAKE_OSX_DEPLOYMENT_TARGET=14.0 \
    -DCMAKE_OSX_ARCHITECTURES="arm64" \
    -DSD_METAL=ON \
    -DSD_BUILD_SHARED_LIBS=OFF \
    -DSD_BUILD_EXAMPLES=OFF \
    -DCMAKE_C_FLAGS="-w" \
    -DCMAKE_CXX_FLAGS="-w"

cmake --build build-ios-sim-arm64 --config Release --target stable-diffusion

# 2b. Simulator x86_64 (Intel)
echo "  - Building Simulator x86_64..."
cmake -S stable-diffusion.cpp -B build-ios-sim-x64 -G Xcode \
    -DCMAKE_SYSTEM_NAME=iOS \
    -DCMAKE_OSX_SYSROOT=iphonesimulator \
    -DCMAKE_OSX_DEPLOYMENT_TARGET=14.0 \
    -DCMAKE_OSX_ARCHITECTURES="x86_64" \
    -DSD_METAL=ON \
    -DSD_BUILD_SHARED_LIBS=OFF \
    -DSD_BUILD_EXAMPLES=OFF \
    -DCMAKE_C_FLAGS="-w" \
    -DCMAKE_CXX_FLAGS="-w"

cmake --build build-ios-sim-x64 --config Release --target stable-diffusion

# 2c. Merge into Fat Libraries
echo "  - Merging into fat libraries..."

# Helper function to find and merge libs
merge_lib() {
    local lib_name=$1
    local arm64_lib=$(find build-ios-sim-arm64 -name "$lib_name" | head -n 1)
    local x64_lib=$(find build-ios-sim-x64 -name "$lib_name" | head -n 1)

    if [ -f "$arm64_lib" ] && [ -f "$x64_lib" ]; then
        echo "    Merging $lib_name..."
        lipo -create "$arm64_lib" "$x64_lib" -output "libs/ios-simulator/$lib_name"
    else
        echo "    WARNING: Could not find $lib_name in both builds. Skipping."
    fi
}

merge_lib "libstable-diffusion.a"
merge_lib "libggml.a"
merge_lib "libggml-base.a"
merge_lib "libggml-cpu.a"
merge_lib "libggml-blas.a"
merge_lib "libggml-metal.a"

echo "--------------------------------------------------------"
echo "iOS build complete."
echo "Device libs (arm64)      : cpp/libs/ios-device"
echo "Simulator libs (arm64/x64): cpp/libs/ios-simulator"
echo "--------------------------------------------------------"
