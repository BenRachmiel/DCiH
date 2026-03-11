#!/bin/bash
# Build the Rust native library for Desktop JVM (current host architecture)
set -euo pipefail

cd "$(dirname "$0")/sudoku-core"
cargo build --release --features uniffi

# Determine the OS-specific library name
case "$(uname -s)" in
    Linux*)  LIB_NAME="libsudoku_core.so" ;;
    Darwin*) LIB_NAME="libsudoku_core.dylib" ;;
    *)       LIB_NAME="sudoku_core.dll" ;;
esac

# Copy to a known location for Gradle to pick up
DEST="../../app/src/desktopMain/resources/native"
mkdir -p "$DEST"
cp "target/release/$LIB_NAME" "$DEST/"
echo "Built $LIB_NAME → $DEST/"
