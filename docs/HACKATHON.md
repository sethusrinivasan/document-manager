# Hackathon Submission: Document Manager

## What is this?

A fully functional, privacy-first Android document manager built entirely with Kiro. The app encrypts documents on-device, supports OCR-powered metadata extraction, natural language search, and runs completely offline with no backend server.

## The Problem it Solves

People store sensitive documents (passports, insurance, medical records) on their phones with no encryption, no organization, and no control over where their data goes. Document Manager gives users:

- **Real encryption** (AES-256-GCM, hardware-backed keys)
- **Automated organization** (auto-tagging, OCR metadata extraction)
- **Zero data collection** (no server, no telemetry without explicit opt-in)
- **Full document lifecycle** (import → organize → search → share → backup)

## How Kiro Was Used

This project was built spec-first using Kiro's structured development workflow:

1. **Requirements** (`.kiro/specs/travel-document-manager/requirements.md`) — 40 formal requirements with acceptance criteria
2. **Design** (`.kiro/specs/travel-document-manager/design.md`) — Component interfaces, data models, security architecture, correctness properties
3. **Tasks** (`.kiro/specs/travel-document-manager/tasks.md`) — Implementation checklist generated from the design
4. **Implementation** — All code generated and refined through Kiro conversations
5. **Testing** — Property-based tests and unit tests

The `.kiro/` directory contains the full spec trail showing how the project evolved from requirements → design → implementation.

## Key Features

| Feature | Description |
|---------|-------------|
| Document Import | Single file, camera scan, local folder, Google Drive, URL import |
| AI OCR | Automatic document classification and metadata extraction (ML Kit) |
| Smart Search | Tag-based, free-text, and natural language ("find my passport", "flight tickets") |
| Encryption | AES-256-GCM per-file + Android KeyStore + biometric auth |
| Per-Document PIN | Optional additional encryption layer per document |
| Sharing | Android share sheet with proper URI grants (WhatsApp, Gmail, etc.) |
| Android Auto | MediaBrowserService for audio file playback in cars |
| WiFi Sharing | Embedded HTTP server with token auth for LAN document management |
| Backup/Restore | Local folder, Google Drive, S3-compatible (encrypted ZIP) |
| DICOM Viewer | Custom parser built from first principles (no library dependencies) |

## How to Build and Run

### Prerequisites
- JDK 17 (Azul Zulu recommended)
- Android SDK API 34, Build Tools 34.0.0
- Android device or emulator (API 26+)

### Build
```bash
# Clone
git clone https://github.com/YOUR_USERNAME/document-manager.git
cd document-manager

# Set environment
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Build debug APK
./gradlew assembleDebug

# APK is at: app/build/outputs/apk/debug/app-debug.apk
```

### Deploy to device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.app.traveldocs/.presentation.MainActivity
```

### Run tests
```bash
./gradlew test
```

## Architecture

Clean Architecture with MVVM:
- **Domain** — Pure Kotlin, no Android deps (models, interfaces, use cases)
- **Data** — Room DB, ML Kit OCR, AES-256 encryption, NanoHTTPD web server
- **Presentation** — Jetpack Compose + Material 3, Hilt ViewModels

## Tech Stack

Kotlin 1.9.22 · Jetpack Compose · Material 3 · Hilt · Room · ML Kit · BouncyCastle · NanoHTTPD · AndroidX Media (Android Auto) · Kotest (property testing)

## What Makes This Special

1. **Privacy as architecture** — Not just a feature toggle. Encryption is the default state. No network calls for core functionality.
2. **DICOM from scratch** — Custom medical image parser with zero dependencies (all existing Android DICOM libs are GPL).
3. **Spec-driven development** — 40 requirements, 25 correctness properties, component interfaces defined before implementation.
4. **Production-ready details** — EULA, crash reporting, input sanitization, Play Store compliance, Android Auto support.
