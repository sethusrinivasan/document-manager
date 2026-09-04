#!/bin/bash
# Source this file before building: source setup-env.sh
# This script works on both Ubuntu (Linux) and macOS

# Detect OS
OS_NAME=""
if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS_NAME="Linux"
elif [[ "$OSTYPE" == "darwin"* ]]; then
    OS_NAME="macOS"
else
    echo "Unsupported OS: $OSTYPE"
    exit 1
fi

# Set default Android SDK path
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-dev-tools/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

# Auto-detect Java home if not set
if [ -z "$JAVA_HOME" ]; then
    if [ "$OS_NAME" == "macOS" ]; then
        # Try common macOS Java locations
        if [ -d "/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home" ]; then
            export JAVA_HOME="/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home"
        elif [ -d "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home" ]; then
            export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
        elif [ -d "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
            export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
        elif [ -d "/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ]; then
            export JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
        elif command_exists java; then
            export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null)
        fi
    else
        # Try common Ubuntu Java locations
        if [ -d "/usr/lib/jvm/zulu17-ca-jdk-amd64" ]; then
            export JAVA_HOME="/usr/lib/jvm/zulu17-ca-jdk-amd64"
        elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
            export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
        elif [ -d "/usr/lib/jvm/temurin-17-jdk-amd64" ]; then
            export JAVA_HOME="/usr/lib/jvm/temurin-17-jdk-amd64"
        elif command_exists java; then
            export JAVA_HOME=$(dirname $(dirname $(readlink $(readlink $(which java)))))
        fi
    fi
fi

# Update PATH with Java and Android tools
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# Print environment configuration
echo "Environment configured for $OS_NAME:"
echo "  JAVA_HOME=$JAVA_HOME"
echo "  ANDROID_HOME=$ANDROID_HOME"
echo "  ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"

# Show Java version if available
if command -v java &> /dev/null; then
    echo "  java: $(java -version 2>&1 | head -1 | tr -d '\r')"
else
    echo "  java: NOT FOUND (set JAVA_HOME)"
fi

# Show ADB version if available
if command -v adb &> /dev/null; then
    echo "  adb:  $(adb version 2>&1 | head -1 | tr -d '\r')"
else
    echo "  adb:  NOT FOUND (install Android SDK Platform Tools)"
fi

echo ""
echo "To use this environment, run: source scripts/setup-env.sh"
echo ""
