#!/bin/bash
# Deploy Document Manager to connected Android device
# Usage: ./scripts/deploy.sh [debug|release]
# Default: release

set -e

VARIANT="${1:-release}"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== Document Manager Deploy ==="
echo "Variant: $VARIANT"
echo "Project: $PROJECT_DIR"
echo ""

# Build
echo "Building $VARIANT APK..."
if [ "$VARIANT" = "debug" ]; then
    "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" assembleDebug --no-daemon
    APK="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
else
    "$PROJECT_DIR/gradlew" -p "$PROJECT_DIR" assembleRelease --no-daemon
    APK="$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk"
fi

if [ ! -f "$APK" ]; then
    echo "ERROR: APK not found at $APK"
    exit 1
fi

echo ""
echo "APK: $APK ($(du -h "$APK" | cut -f1))"

# Check device
if ! adb devices | grep -q "device$"; then
    echo ""
    echo "ERROR: No Android device connected."
    echo "Connect via USB and enable USB debugging, then retry."
    exit 1
fi

# Install
echo ""
echo "Installing on device..."
adb install -r "$APK"

# Launch
echo "Launching..."
adb shell am start -n com.app.traveldocs/.presentation.MainActivity

echo ""
echo "=== Done ==="
