#!/bin/bash
# Script to generate release keystore for Play Store signing

KEYSTORE_PATH="$HOME/release.keystore"
ALIAS="upload_key"
VALIDITY_DAYS=10000

echo "Generating release keystore for Google Play Store..."
echo "Keystore will be created at: $KEYSTORE_PATH"
echo ""

# Check if keystore already exists
if [ -f "$KEYSTORE_PATH" ]; then
    echo "Keystore already exists at: $KEYSTORE_PATH"
    read -p "Overwrite? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Aborted. Using existing keystore."
        exit 0
    fi
fi

# Generate the keystore
keytool -genkey -v -keystore "$KEYSTORE_PATH" \
    -storepass documentmanager2024 \
    -keypass documentmanager2024 \
    -keyalg RSA \
    -keysize 2048 \
    -validity $VALIDITY_DAYS \
    -alias "$ALIAS" \
    -dname "CN=Document Manager, OU=Development, O=Document Manager, L=Unknown, ST=Unknown, C=US"

echo ""
echo "========================================"
echo "Release keystore generated successfully!"
echo "========================================"
echo ""
echo "IMPORTANT: Store this file securely and backup in multiple locations."
echo "If you lose this keystore, you cannot update your app on Play Store."
echo ""
echo "Keystore location: $KEYSTORE_PATH"
echo "Keystore password: documentmanager2024"
echo "Key alias: $ALIAS"
echo "Key password: documentmanager2024"
echo ""
echo "The build.gradle.kts file has been updated with these credentials."
echo "Run './gradlew assembleRelease' to build your signed APK."
