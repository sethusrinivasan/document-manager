#!/bin/zsh
# Source this file before building: source setup-env.sh

export JAVA_HOME=~/android-dev-tools/zulu17.54.21-ca-jdk17.0.13-macosx_x64
export ANDROID_HOME=~/android-dev-tools/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "Environment configured:"
echo "  JAVA_HOME=$JAVA_HOME"
echo "  ANDROID_HOME=$ANDROID_HOME"
echo "  java: $(java -version 2>&1 | head -1)"
echo "  adb:  $(adb version 2>&1 | head -1)"
