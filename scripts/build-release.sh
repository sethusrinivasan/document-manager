#!/bin/bash
# Script to build release APK and AAB for Play Store

set -e

echo "========================================"
echo "  DocVault Release Build"
echo "========================================"
echo ""

# Check for release keystore
if [ ! -f "$HOME/release.keystore" ]; then
    echo -e "\033[0;31mERROR: Release keystore not found!\033[0m"
    echo "Generate it first with: ./scripts/release_keystore.sh"
    exit 1
fi

echo "✓ Release keystore found"
echo ""

# Build release APK
echo "Building release APK..."
./gradlew assembleRelease
if [ $? -eq 0 ]; then
    echo -e "\033[0;32m✓ Release APK built successfully\033[0m"
    echo ""
    ls -lh app/build/outputs/apk/release/*.apk
else
    echo -e "\033[0;31m✗ Release APK build failed\033[0m"
    exit 1
fi

echo ""

# Build release App Bundle (for Play Store)
echo "Building release App Bundle..."
./gradlew bundleRelease
if [ $? -eq 0 ]; then
    echo -e "\033[0;32m✓ Release App Bundle built successfully\033[0m"
    echo ""
    ls -lh app/build/outputs/bundle/release/*.aab
else
    echo -e "\033[0;31m✗ Release App Bundle build failed\033[0m"
    exit 1
fi

echo ""
echo "========================================"
echo "  Build Complete!"
echo "========================================"
echo ""
echo "APK location:  app/build/outputs/apk/release/"
echo "AAB location:  app/build/outputs/bundle/release/"
echo ""
echo "To deploy APK:"
echo "  adb install app/build/outputs/apk/release/app-release.apk"
echo ""
echo "To upload to Play Store:"
echo "  Upload app/build/outputs/bundle/release/app-release.aab"
