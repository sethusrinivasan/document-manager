# Hackathon Submission: Document Manager

**Repository:** https://github.com/sethusrinivasan/document-manager
**Download APK:** https://github.com/sethusrinivasan/document-manager/releases

## What is this?

A privacy-first Android document manager. Encrypts documents on-device, organizes them with on-device OCR, and runs completely offline with no backend server. No cloud, no accounts, no compromises.

## The Problem it Solves

People store sensitive documents (passports, insurance, medical records) on their phones with no encryption, no organization, and no control over where their data goes. Document Manager gives users:

- Real encryption (AES-256-GCM, hardware-backed keys)
- Automated organization (on-device OCR for classification and tagging)
- Zero data collection (no server, no telemetry without explicit opt-in)
- Full document lifecycle (import → organize → search → share → backup)

## How Kiro Was Used

This project was built spec-first using Kiro's structured development workflow:

1. **Requirements** (`.kiro/specs/travel-document-manager/requirements.md`) — 50 formal requirements with acceptance criteria
2. **Design** (`.kiro/specs/travel-document-manager/design.md`) — Component interfaces, data models, security architecture
3. **Tasks** (`.kiro/specs/travel-document-manager/tasks.md`) — 70 implementation task groups, all completed
4. **Implementation** — All code generated and refined through Kiro conversations
5. **Testing** — 120 unit tests passing, property-based tests for domain logic

The `.kiro/` directory contains the full spec trail.

## Stable Features

| Feature | Description |
|---------|-------------|
| Document Import | Single file, camera scan, local folders with recursive subfolder tagging |
| On-device OCR | ML Kit text recognition for automatic document classification |
| Encrypted Storage | AES-256-GCM per-file, keys in Android KeyStore, biometric auth |
| Per-Document PIN | Additional PBKDF2-derived encryption per document (non-recoverable) |
| Tag Organization | Auto-tagging from OCR + folder structure, color-coded, folder view on home |
| Search | Free-text search across all documents |
| Sharing | Android share sheet with proper URI permission grants |
| Dark Theme | Immediate toggle, no restart needed |
| Review & Classify | Batch-tag untagged docs, review low-confidence OCR results |
| Share Target | Receive documents from other apps directly |
| EULA & Consent | Legal agreement with timestamp + location capture |
| Diagnostics | Run DB consistency checks, share debug logs as ZIP |

## Experimental Features (Preview)

| Feature | Description |
|---------|-------------|
| Backup & Restore | Password-protected ZIP, detailed restore progress reporting |
| Google Drive Import | Browse and import from Drive folders |
| S3 Backup | Backup to any S3-compatible endpoint |
| WiFi Document Sharing | Embedded HTTP server with token auth for LAN management |
| Android Auto | MediaBrowserService for audio file playback in cars |
| DICOM Viewer | Custom medical image parser (no third-party dependencies) |
| Import from URL | Download files from HTTP/HTTPS with auto-format detection |
| Audio Playback | MP3/M4A import with system media controls |

## How to Run

### Download (no build required)

1. Download `document-manager-debug.apk` from [Releases](https://github.com/sethusrinivasan/document-manager/releases)
2. Transfer to Android phone, install (enable "Unknown sources" if prompted)

### Build from source

```bash
git clone https://github.com/sethusrinivasan/document-manager.git
cd document-manager
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug
# APK at: app/build/outputs/apk/debug/app-debug.apk
```

### Deploy to device

```bash
./scripts/deploy.sh debug
```

### Run tests

```bash
./gradlew testDebugUnitTest  # 120 tests, all passing
```

## Architecture

Clean Architecture (MVVM):
- **Domain** — Pure Kotlin models, interfaces, use cases (no Android deps)
- **Data** — Room DB, ML Kit OCR, AES-256 encryption, NanoHTTPD web server
- **Presentation** — Jetpack Compose + Material 3, Hilt ViewModels

## Tech Stack

Kotlin 1.9.22 · Jetpack Compose · Material 3 · Hilt · Room 2.6.1 · ML Kit · BouncyCastle · NanoHTTPD · AndroidX Media · Zip4j · Kotest

## What Makes This Special

1. **Privacy as architecture** — Encryption is the default state. No network calls for core functionality. Zero data leaves the device without explicit user action.
2. **DICOM from scratch** — Custom medical image parser (all existing Android DICOM libs are GPL incompatible).
3. **Spec-driven development** — 50 requirements, 70 task groups, component interfaces defined before implementation. Full `.kiro/` trail.
4. **Production details** — EULA, crash reporting, input sanitization, Android Auto, per-document PIN, Play Store compliance review.
5. **Built with AI, designed by human** — All processing happens locally. Classification errors documented honestly. Community invited to improve.
