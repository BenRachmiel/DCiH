#!/bin/bash
# Build the Rust native library for Android via cargo-ndk
# Requires: cargo install cargo-ndk && rustup target add aarch64-linux-android x86_64-linux-android
set -euo pipefail

cd "$(dirname "$0")/sudoku-core"

# Build for arm64 and x86_64
cargo ndk --target aarch64-linux-android --target x86_64-linux-android \
    --platform 24 \
    build --release --features uniffi

# Copy to jniLibs
JNILIBS="../../app/src/androidMain/jniLibs"
mkdir -p "$JNILIBS/arm64-v8a" "$JNILIBS/x86_64"
cp "target/aarch64-linux-android/release/libsudoku_core.so" "$JNILIBS/arm64-v8a/"
cp "target/x86_64-linux-android/release/libsudoku_core.so" "$JNILIBS/x86_64/"
echo "Built Android libs → $JNILIBS/"
