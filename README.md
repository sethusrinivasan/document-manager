# Document Manager

Private, encrypted document storage for Android. No cloud, no accounts, no compromises.

## Why this exists

I needed a place to keep passport scans, insurance docs, and travel papers on my phone where:
- They're encrypted and locked behind biometrics
- Nothing gets uploaded anywhere without me explicitly doing it
- I can find stuff fast with tags and search
- Large PDFs don't hang the app

So I built this. It's open source, free to use, and the community is welcome to improve it.

## What it does

- **Import** documents from files, camera, or local folders
- **OCR** extracts text and classifies docs automatically (passport, visa, ticket, etc.)
- **Encrypt** everything with AES-256-GCM, keys in Android KeyStore
- **Search** by tags and free-text across all documents
- **Share** via the standard Android share sheet — decrypted on the way out
- **Backup & Restore** to local folder with password-protected ZIP
- **Per-document PIN** — lock individual sensitive documents with an additional PIN
- **Folder import** with automatic tagging from subfolder names
- **Dark theme** — toggle in settings, applies immediately
- **Google Drive import** *(experimental)* — import from Drive folders
- **S3-compatible backup** *(experimental)* — backup to AWS S3, MinIO, Backblaze
- **Audio playback & Android Auto** *(experimental)* — MP3/M4A import with car head unit support
- **GPS tagging** *(experimental)* — capture location on import, background tracking
- **WiFi sharing** *(experimental)* — embedded web server for LAN document management
- **Import from URL** *(experimental)* — download and import files from any web address
- **DICOM viewer** *(experimental)* — medical image preview with custom parser

## Principles

1. Your documents never leave your phone unless YOU share/backup them
2. No telemetry without explicit opt-in consent
3. GPS tracking is off by default, behind a feature flag
4. Experimental features are hidden until you turn them on
5. The app works fully offline — network is only for optional cloud backup

## Built with AI

This app was built and tested with AI assistance (Kiro). All document processing (OCR, classification, tagging) happens locally on your device using third-party libraries (ML Kit, etc.). Classification errors can and will occur due to the quality and limitations of those libraries — the app makes no guarantee of accuracy. Always verify extracted data independently before relying on it.

The code is real, it compiles, it runs. If you spot something that could be cleaner, more idiomatic, or just better: PRs and issues are welcome.

## Quick start

```bash
# You need JDK 17 and Android SDK (API 34)
source scripts/setup-env.sh   # or set JAVA_HOME and ANDROID_HOME manually

./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

See the [full setup guide](#development-setup) below if you need to install the toolchain from scratch.

## Project layout

```
app/src/main/java/com/app/traveldocs/
├── domain/            # Business logic. Pure Kotlin. No Android deps.
│   ├── model/         # Document, Tag, SearchResult, etc.
│   ├── repository/    # Interfaces only
│   └── usecase/       # Import pipeline, search orchestration
├── data/              # The dirty work. Room, ML Kit, filesystem, crypto.
│   ├── local/         # DB, encryption, auth, feature flags
│   ├── scanner/       # ML Kit OCR wrapper
│   ├── nlp/           # Regex-based travel query parser
│   ├── dicom/         # Custom DICOM parser (no deps, from first principles)
│   └── backup/        # ZIP packaging, Drive/S3 upload
├── presentation/      # Compose screens + ViewModels
│   ├── documents/     # Import, list, viewer, batch import
│   ├── search/        # Search screen
│   ├── settings/      # Feature flags, preferences
│   └── onboarding/    # Disclaimer, consent, splash
├── debug/             # Logger, crash handler, GPS service, telemetry
└── di/                # Hilt modules
```

The domain layer has zero Android imports. Data implements domain interfaces. Presentation talks to domain via ViewModels. Standard clean architecture, nothing exotic.

## Tech choices and why

| Choice | Why |
|--------|-----|
| Compose + Material 3 | Declarative, testable, good accessibility support out of the box |
| Room (no SQLCipher) | Files are encrypted individually. DB only has metadata. SQLCipher added 14MB and a 16KB alignment headache. |
| AES-256-GCM per file | Each doc encrypted separately. KeyStore-backed. Losing one file doesn't compromise others. |
| Biometric auth | Simpler than custom PIN, harder to bypass, zero crypto bugs from us |
| ML Kit on-device | Offline OCR requirement. No API keys. Google maintains it. |
| Regex NLP (not LLM) | For travel queries. Predictable, testable, no model downloads. Handles the constrained vocab fine. |
| Hilt | Standard Android DI. ViewModels get auto-scoped. |
| Kotest property tests | Domain invariants verified with random inputs, not just cherry-picked examples |

## Feature flags

All experimental stuff is behind toggles in Settings → Experimental Features:

- **Google Drive** — Drive backup/import
- **S3 Storage** — Any S3-compatible endpoint
- **Backup & Restore** — The backup menu item itself
- **GPS Tracking** — Background location logging
- **Extended Formats** — WebP, HEIC, BMP, GIF, DICOM support

Everything is OFF by default. Users see only stable features until they opt in.

## The DICOM thing

Yeah, there's a DICOM viewer. Built from scratch — no library. Parses the tag-length-value structure, extracts pixel data at (7FE0,0010), applies window/level for grayscale, handles 8/12/16-bit and RGB. It's not a medical-grade viewer (no JPEG2000 compressed DICOMs) but it handles uncompressed studies fine for personal reference.

## Known rough edges

- PDF zoom: works but `PdfRenderer` has thread affinity constraints — pages render sequentially
- Google Drive auth: flaky on some devices (Google's SDK issue, not ours)
- HEIC: only works on API 28+ (covers ~95% of devices)
- The NLP parser is a glorified regex engine — it handles travel-related queries but don't ask it philosophy questions

## Development setup

### Prerequisites

- **JDK 17** — Azul Zulu works well: `brew install --cask zulu17`
- **Android SDK** — API 34, Build Tools 34.0.0
- **A phone or emulator** — Tested on Pixel 10a (arm64)

### Environment

```bash
export JAVA_HOME=~/android-dev-tools/zulu17.54.21-ca-jdk17.0.13-macosx_x64
export ANDROID_HOME=~/android-dev-tools/android-sdk
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### Build & deploy

```bash
# One-command build + deploy (default: release)
./scripts/deploy.sh

# Or specify variant
./scripts/deploy.sh release   # production build (minified, no debug logs)
./scripts/deploy.sh debug     # development build (debug logs, diagnostics)

# Manual build only (no deploy)
./gradlew assembleDebug       # debug APK
./gradlew assembleRelease     # release APK (unsigned)
```

### Tests

```bash
./gradlew test                             # all unit + property tests
./gradlew test --tests "*.properties.*"    # property tests only
```

### Debugging

```bash
# Pull all logs from device
./scripts/pull-logs.sh

# Live logcat
adb logcat -s TravelDocs                   # live logs
adb shell run-as com.app.traveldocs cat files/debug_logs/traveldocs_debug.log
```

Or just tap the 🐛 icon in the app — there's a full log viewer built in.

## Contributing

Fork it, branch off `main`, make your changes, open a PR. Keep commits focused and messages in imperative mood.

Things that would be particularly useful:
- Compressed DICOM support (JPEG2000, RLE)
- Better NLP parser (maybe a small on-device model?)
- UI/UX polish — animations, transitions, dark theme refinement
- Accessibility audit
- Integration tests with real document fixtures

## Security notes

- Documents encrypted at rest (AES-256-GCM, key in hardware KeyStore)
- Temp files cleaned on app pause
- No cleartext HTTP (network security config enforced)
- Debug logging disabled in release builds
- PIN/key material zeroed after use
- Crash reports stored locally only — user manually sends via email if they choose

## Docs

| Doc | What's in it |
|-----|-------------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System overview, layer architecture, data flows, security model, DB schema |
| [docs/wireframes.md](docs/wireframes.md) | ASCII wireframes for all screens |
| [.kiro/specs/…/requirements.md](.kiro/specs/travel-document-manager/requirements.md) | 40 requirements with acceptance criteria |
| [.kiro/specs/…/design.md](.kiro/specs/travel-document-manager/design.md) | Component interfaces, algorithms |
| [PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md) | Privacy policy (required for Play Store) |

## License

Apache 2.0. See [LICENSE](LICENSE).

## Acknowledgments

- [ML Kit](https://developers.google.com/ml-kit) — OCR engine
- [BouncyCastle](https://www.bouncycastle.org/) — Argon2id hashing
- [Material Icons](https://fonts.google.com/icons) — Icon set (Apache 2.0)
- [Kotest](https://kotest.io/) — Property-based testing
- [Zip4j](https://github.com/srikanth-lingala/zip4j) — Password-protected ZIP archives
# document-manager
