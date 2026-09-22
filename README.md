# Paperstow

[![Build Android App](https://github.com/sethusrinivasan/document-manager/actions/workflows/build.yml/badge.svg)](https://github.com/sethusrinivasan/document-manager/actions/workflows/build.yml)
[![CodeQL Analysis](https://github.com/sethusrinivasan/document-manager/actions/workflows/codeql.yml/badge.svg)](https://github.com/sethusrinivasan/document-manager/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android%208%2B-green.svg)](docs/PLAY_STORE.md)
[![API](https://img.shields.io/badge/minSdk-26-informational.svg)](app/build.gradle.kts)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Latest release](https://img.shields.io/github/v/release/sethusrinivasan/document-manager)](https://github.com/sethusrinivasan/document-manager/releases/latest)
[![Built with Kiro](https://img.shields.io/badge/Built%20with-Kiro-FF6B00)](https://kiro.dev)
[![Built with Cursor](https://img.shields.io/badge/Built%20with-Cursor-000000)](https://cursor.com)

Keep a copy of your family travel papers on this device. Encrypted. No account. No cloud.

**Package:** `com.app.paperstow` · **Version:** 1.1.0 (`versionCode` 3)

## Why this exists

A place to keep passport scans, visas, tickets, and insurance on the phone where:

- They are encrypted and unlocked with the device's fingerprint, face, or screen lock
- Nothing is uploaded unless you share a file or export an archive
- You can find a page with tags and on-device OCR
- Large PDFs do not hang the app

Paperstow is open source (Apache 2.0). Issues and PRs are welcome.

## What it does

- **Import** a file, the on-device document scanner / camera, or a local folder (subfolder names become tags). Other apps can also share PDFs, images, and text into Paperstow.
- **OCR** (bundled ML Kit Latin) reads photos and PDF pages, suggests a type (passport, visa, ticket, and similar), and stores extracted text for search.
- **Encrypt** each file with AES-256-GCM. Keys stay in Android KeyStore.
- **Notes and checklists** you write on the phone, with tags.
- **Search** by filename, tags, and extracted page text. Settings can rebuild the index with progress.
- **Share** through Android's share sheet.
- **Archive and restore** a ZIP to a folder you pick. Password is optional (blank = unencrypted ZIP). Restore asks for a password only if the archive is protected. Restore verifies the backup database before swapping it in.
- **My Trail** (optional) keeps unique places from the last 24 hours on this phone, with battery at each save. You can include it in a backup as GPX. Locations are not uploaded.
- **Dark theme**, show/hide home tips, and **Reset App** (Settings → Danger zone).
- **About** shows version, package name, and the CycloneDX SBOM generated every build and packaged in the APK.

There is **no** Google Drive, S3, Wi-Fi share, Android Auto, URL import, or experimental-feature master switch. Those were removed from the Play build. Leftover `FeatureFlags` readers return off / backup-always-on so old preference files do not crash.

Limits that still apply: 100 documents per family member, 20 tags per document, 500 files per folder import.

## Principles

1. Documents never leave the phone unless you share or export them.
2. No telemetry without an explicit opt-in. Counts stay on the device.
3. Core use works offline. Internet is only for an optional ML Kit / scanner model refresh.

## Built with AI

This app was built and tested with AI assistance: first [Kiro](https://kiro.dev) (spec-first generation), then [Cursor](https://cursor.com). All document processing (OCR, classification, tagging) happens on the device using third-party libraries (ML Kit and others). Classification errors can and will occur. Always verify extracted data against the original page.

The code compiles and runs. If something could be cleaner: PRs and issues are welcome.

## Demo

Emulator walkthrough of Paperstow 1.1 after loading the sample trip. Home, a tag folder, preview, import, search, checklist, backup, and About.

<video src="docs/demo/paperstow-emulator-demo.mp4" controls width="360" poster="docs/screenshots/2.jpg">
  <a href="docs/demo/paperstow-emulator-demo.mp4">Play the demo video</a>
</video>

If the player does not show: [docs/demo/paperstow-emulator-demo.mp4](docs/demo/paperstow-emulator-demo.mp4)

## Screenshots

Captured from the `Paperstow_API36` emulator after loading the sample trip. See [`docs/screenshots/MANIFEST.txt`](docs/screenshots/MANIFEST.txt).

| | | |
|:---:|:---:|:---:|
| ![1](docs/screenshots/1.jpg) | ![2](docs/screenshots/2.jpg) | ![3](docs/screenshots/3.jpg) |
| Splash | Home (sample trip) | Health tag folder |
| ![4](docs/screenshots/4.jpg) | ![5](docs/screenshots/5.jpg) | ![6](docs/screenshots/6.jpg) |
| Document preview | All documents | Import |
| ![7](docs/screenshots/7.jpg) | ![8](docs/screenshots/8.jpg) | ![9](docs/screenshots/9.jpg) |
| Search | Write a note | Checklist |
| ![10](docs/screenshots/10.jpg) | ![11](docs/screenshots/11.jpg) | ![12](docs/screenshots/12.jpg) |
| Manage tags | Backup (password optional) | Restore |
| ![13](docs/screenshots/13.jpg) | ![14](docs/screenshots/14.jpg) | ![15](docs/screenshots/15.jpg) |
| Settings | About | SBOM |
| ![16](docs/screenshots/16.jpg) | | |
| My Trail | | |

## Download

CI on `main` publishes GitHub Releases. The workflow runs `assembleDebug` and uploads:

| File | What it is |
|------|------------|
| `document-manager-debug.apk` | Debug APK from `app/build/outputs/apk/debug/app-debug.apk` |
| `sbom.json` | CycloneDX 1.5 SBOM for that build (when generated) |

A `document-manager-release.apk` appears only if a release APK was produced in that job. Default CI does **not** run `assembleRelease`. For a minified Play build, use `./scripts/build-release.sh` locally.

1. Open [Releases](https://github.com/sethusrinivasan/document-manager/releases)
2. Download the debug APK (or a release APK if present)
3. On Android 8.0+ (API 26), enable install from that browser or file manager, then install

`com.app.paperstow` is a new applicationId. It does not upgrade an older `com.app.traveldocs` install.

### Install via USB or wireless debugging

```bash
# USB (Developer options → USB debugging)
adb install document-manager-debug.apk

# Or from a clone:
./scripts/deploy.sh debug

# Android 11+ wireless debugging
adb pair <ip>:<port>
adb connect <ip>:<port>
adb install document-manager-debug.apk
```

After install: accept the EULA, unlock with the device biometric or screen lock, then Import (or overflow → Load sample trip).

## Quick start (from source)

```bash
# JDK 17 and Android SDK (compile/target API 36)
bash scripts/setup.sh
./gradlew assembleDebug
./scripts/deploy.sh debug
```

`scripts/setup.sh` installs AVD `Paperstow_API36` if it is missing.

## Project layout

```
app/src/main/java/com/app/paperstow/
├── domain/              # Models, repository interfaces, import use case. No Android UI.
│   ├── model/
│   ├── repository/
│   ├── usecase/
│   └── safety/          # My Trail uniqueness + GPX
├── data/
│   ├── local/           # Room (traveldocs.db v4), crypto, search index, feature-flag stubs
│   ├── importer/        # File / folder import
│   ├── scanner/         # ML Kit OCR + document scanner
│   ├── nlp/             # Regex travel parser + checklist generator
│   ├── tags/
│   ├── backup/          # ZIP archive / restore (optional AES), Room file swap
│   ├── safety/          # My Trail foreground service
│   └── demo/            # Sample trip
├── presentation/        # Compose screens + ViewModels
│   ├── documents/       # Import, list, viewer, notes, checklists
│   ├── search/, tags/, settings/, backup/, about/
│   ├── safety/, review/, feedback/, diagnostics/, onboarding/
└── debug/               # Logger, crash handler, optional local telemetry
```

`data/dicom`, `data/drive`, `data/webserver`, and `media` still exist as leftover source. They are not offered in the UI. Room database filename stays `traveldocs.db` so older backups can restore.

## Tech choices

| Choice | Why |
|--------|-----|
| Compose + Material 3 | Single-activity UI |
| Room without SQLCipher | Each file is encrypted. The DB holds metadata, OCR text, tags, and My Trail points. SQLCipher added size and alignment cost. |
| AES-256-GCM per file | KeyStore-backed. Losing one file does not expose others. |
| Transportable backup ZIP | Archive decrypts files into the ZIP so another phone can restore. Optional Zip4j AES password. |
| BiometricPrompt | Device fingerprint / face / screen lock. No separate app PIN. |
| Bundled ML Kit Latin | Offline OCR for photos and the first four PDF pages (rendered on a white bitmap). |
| Regex NLP | Constrained travel queries. No on-device LLM. |
| Hilt | Standard Android DI |
| Kotest property tests | Domain invariants with random inputs |
| CycloneDX SBOM | Generated per variant into assets; About can show it |

## Known rough edges

- PDF pages render one at a time (`PdfRenderer` thread affinity).
- HEIC import needs API 28+.
- The NLP parser is regex. It will not answer open-ended questions.
- Folder import over SAF is slow on very large trees (Binder per file); work runs on `Dispatchers.IO`.
- A leftover `com.app.traveldocs` install is a separate app. Data and KeyStore keys do not migrate automatically.

Library-level notes: [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md).

## Development

### Prerequisites

- JDK 17 (Temurin or Azul Zulu)
- Android SDK API 36, Build Tools 35.0.0
- Phone or emulator (`Paperstow_API36`)

```bash
bash scripts/setup.sh
# Linux example if you skip the script:
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=$HOME/android-dev-tools/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### Build

```bash
./gradlew assembleDebug
./gradlew assembleRelease          # unsigned unless ~/release.keystore is present
./scripts/build-release.sh         # APK + AAB for Play
./scripts/deploy.sh debug          # default; starts the AVD if needed
./scripts/deploy.sh release
./gradlew testDebugUnitTest
./gradlew test --tests "*.properties.*"
./scripts/pull-logs.sh
adb logcat -s TravelDocs
adb shell run-as com.app.paperstow cat files/debug_logs/traveldocs_debug.log
```

Play signing: `./scripts/release_keystore.sh` then `DOCVAULT_*` env vars (see [docs/PLAY_STORE.md](docs/PLAY_STORE.md)).

CI (`.github/workflows/build.yml`) uses `android-actions/setup-android@v4` with `packages: platform-tools`. Do not revert to v3 — its default `tools` package is gone and `sdkmanager` exits 1.

## Contributing

Fork, branch from `main`, open a PR. Keep commits focused.

Useful work: compressed DICOM (not in the current UI), a better on-device parser, UI polish, accessibility, and instrumentation tests with real fixtures.

## Security

- Files at rest: AES-256-GCM, key in hardware KeyStore when the device supports it
- Archive ZIP may contain **plaintext** document bytes (so another device can restore). Use a password if the ZIP will leave your control.
- Temp share files are cleaned on pause
- Cleartext HTTP is blocked by network security config
- Debug logging is off in release
- Crash reports stay local unless you email them

How to report a vulnerability: [SECURITY.md](SECURITY.md).

## Docs

| Doc | What's in it |
|-----|-------------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, import pipeline, schema, backup, My Trail |
| [docs/PLAY_STORE.md](docs/PLAY_STORE.md) | Listing copy, permissions, signing |
| [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md) | Privacy policy (source) |
| [docs/privacy.html](docs/privacy.html) | Same policy for GitHub Pages |
| [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) | Room WAL, PdfRenderer, Zip4j, ML Kit |
| [docs/THIRD_PARTY_LICENSES.md](docs/THIRD_PARTY_LICENSES.md) | Dependency licenses |
| [docs/wireframes.md](docs/wireframes.md) | Current screen map + historical ASCII |
| [LICENSE](LICENSE) / [NOTICE](NOTICE) | Apache 2.0 + attribution |
| [docs/demo/paperstow-emulator-demo.mp4](docs/demo/paperstow-emulator-demo.mp4) | Emulator demo (sample trip) |
| [docs/KIRO_GENERATION_PROMPT.md](docs/KIRO_GENERATION_PROMPT.md) | Historical generation prompt |
| [.kiro/specs/…](.kiro/specs/travel-document-manager/requirements.md) | Original spec (not the shipping feature set) |

## License

Paperstow source is **Apache License 2.0**. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

Third-party components keep their own terms (Apache 2.0, Bouncy Castle MIT-style, Google ML Kit / Play services, EPL-2.0 JUnit on the test classpath). Inventory: [docs/THIRD_PARTY_LICENSES.md](docs/THIRD_PARTY_LICENSES.md).

## Acknowledgments

- [AndroidX](https://developer.android.com/jetpack/androidx) / [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [ML Kit](https://developers.google.com/ml-kit) — on-device OCR and document scanner
- [Bouncy Castle](https://www.bouncycastle.org/) — Argon2id / HKDF
- [Zip4j](https://github.com/srikanth-lingala/zip4j) — optional AES ZIP
- [Hilt](https://dagger.dev/hilt/)
- [Kotest](https://kotest.io/)
