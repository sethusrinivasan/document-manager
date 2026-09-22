#!/bin/bash
# Deploy Paperstow to a connected device, or start the local emulator AVD.
# Usage: ./scripts/deploy.sh [debug|release]
# Default: debug (for manual testing)

set -e

VARIANT="${1:-debug}"
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
AVD_NAME="${AVD_NAME:-Paperstow_API36}"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-dev-tools/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

echo "=== Paperstow Deploy ==="
echo "Variant: $VARIANT"
echo "Project: $PROJECT_DIR"
echo ""

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

adb start-server >/dev/null

if ! adb devices | grep -q $'\tdevice$'; then
    if [ ! -x "$ANDROID_HOME/emulator/emulator" ]; then
        echo "ERROR: No device connected and emulator is not installed."
        echo "Run: bash scripts/setup.sh"
        exit 1
    fi
    if [ ! -f "$HOME/.android/avd/${AVD_NAME}.ini" ]; then
        echo "ERROR: AVD $AVD_NAME not found. Run: bash scripts/setup.sh"
        exit 1
    fi
    echo "No device connected — starting $AVD_NAME"
    # Do not relaunch if this AVD is already booting.
    if pgrep -f "qemu-system.*${AVD_NAME}|emulator.*-avd ${AVD_NAME}" >/dev/null 2>&1; then
        echo "Emulator process already running"
    else
        nohup "$ANDROID_HOME/emulator/emulator" -avd "$AVD_NAME" -gpu auto -no-snapshot-save \
            >"$HOME/.android/${AVD_NAME}.log" 2>&1 &
    fi
    echo "Waiting for emulator to finish booting..."
    adb wait-for-device
    for _ in $(seq 1 90); do
        if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
            break
        fi
        sleep 2
    done
fi

if ! adb devices | grep -q $'\tdevice$'; then
    echo "ERROR: Emulator did not reach device state. See $HOME/.android/${AVD_NAME}.log"
    exit 1
fi

echo ""
echo "Installing on device..."
adb install -r "$APK"

echo "Launching..."
adb shell am start -n com.app.paperstow/.presentation.MainActivity

echo ""
echo "=== Done ==="
