# Paperstow — architecture

**App:** Paperstow · **Package:** `com.app.paperstow` · **Version:** 1.1.0 (`versionCode` 3)

On-device vault for travel papers. No Paperstow account or servers. Documents enter through Compose screens, go through domain use cases, and are stored encrypted on disk. Hilt wires the layers. Debug tooling does not change production crypto.

```
┌─────────────────────────────────────────────────────────────────┐
│                        ANDROID DEVICE                             │
├─────────────────────────────────────────────────────────────────┤
│  Presentation (Compose)  →  Domain (Kotlin)  →  Data            │
│  Screens + ViewModels       Use cases + models   Room, ML Kit,  │
│                             repository ifaces    FS, Zip4j      │
│                                                                 │
│  Debug: logger, optional local telemetry, crash handler         │
│  Platform: KeyStore, BiometricPrompt, optional location (Trail) │
├─────────────────────────────────────────────────────────────────┤
│  /data/data/com.app.paperstow/                                  │
│  ├── databases/traveldocs.db     Room v4 (WAL). Name kept for   │
│  │                               older backup restores.         │
│  ├── files/docs/{member}/{id}.enc   AES-256-GCM                 │
│  ├── files/debug_logs/                                          │
│  └── shared_prefs/               settings, consent, tips        │
└─────────────────────────────────────────────────────────────────┘
```

## Layers

### Domain (`domain/`)

Pure Kotlin except `android.net.Uri` on a few interfaces.

```
domain/model/          Document, Tag, DocumentFormat, SearchResult, …
domain/repository/     Interfaces only
domain/usecase/        DocumentImportUseCase
domain/safety/         UniqueLocations, GpxTrail
```

Domain does not import `data/` or `presentation/`.

### Data (`data/`)

```
local/          Room DAOs/entities, DeviceKeyManager, search index, FeatureFlags stubs
importer/       File / folder import + format validation
scanner/        MlKitMetadataExtractor, CameraDocumentScanner
nlp/            RegexNaturalLanguageParser, BasicDocumentChecklistGenerator
tags/           AutoTagGeneratorImpl
backup/         BackupManager, BackupRestore, RoomDbFiles
safety/         SafetyTrailService + repository
demo/           Sample trip
```

Leftover trees (`dicom/`, `drive/`, `webserver/`, `media/`) are not wired into the shipping UI.

`FeatureFlags` readers remain so old SharedPreferences do not crash. Drive / S3 / GPS-tracking / Wi-Fi / experimental return `false`. Backup/restore is always on.

### Presentation

Single `MainActivity` with string-based screen routing (rotation-safe; splash skip is remembered).

Home overflow: Tags, Review & Classify, My Trail, Backup, Restore, sample trip, Settings, Feedback, About.

Settings: home title, telemetry opt-in, dark theme, show/hide tips, rebuild search index, Reset App (two-step confirm).

### Debug

`DebugLogger` (logcat + ring buffer + rotating file), in-app log viewer, `CrashHandler`, consent-gated `UsageTelemetry`. Release builds do not write debug logs.

## Import pipeline

```
User picks file / scan / folder / incoming ACTION_SEND
    → read bytes, detect DocumentFormat, SHA-256
    → duplicate filename → Replace / Cancel
    → DocumentImportUseCase
         1. Encrypt to files/docs/{memberId}/{fileId}.enc
         2. OCR images and first 4 PDF pages (white-filled bitmaps).
            Raw PDF bytes are not sent to ML Kit.
         3. Classify + regex metadata + auto tags
         4. Room insert (documents + metadata + tags)
            Capacity: 100 documents per member, 20 tags per document
            Folder import cap: 500 files
```

OCR failure still stores the file and marks `requiresManualReview`. Search uses `documents.ocrText` (added in Room v4). Settings → Rebuild search index re-reads files on device.

There is **no** GPS stamp on import. Location is used only if the user starts My Trail.

## Security

**Unlock:** `BiometricPrompt` with `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` at launch.

**At rest:** AES-256-GCM per file. KeyStore alias `travel_docs_file_encryption_key`. On-disk layout: 12-byte IV + ciphertext.

**Backup ZIP is transportable.** `BackupManager` decrypts `.enc` files into the archive (plus Room DB, manifest, optional `trail/my_trail.gpx`). A blank password writes an unencrypted ZIP. A password of 4+ characters uses Zip4j AES-256. Treat an unprotected ZIP as plaintext documents.

**Restore** inspects the ZIP, requires a password only if the archive is encrypted, and verifies the backup database contains a `documents` table before replacing the live DB (WAL checkpoint, delete `-wal`/`-shm`, then swap).

**Share:** decrypt to cache, `FileProvider` (`${applicationId}.fileprovider`), `ACTION_SEND`. Cache wiped on pause.

## Database (Room v4)

```sql
-- documents  (+ ocrText TEXT NOT NULL DEFAULT '' since v4)
-- document_metadata
-- document_tags
-- family_members   -- schema leftover; unlock is device biometric, not per-member PIN
-- gps_tracks       -- My Trail points (lat, lon, accuracy, timestamp, batteryPercent)
```

Migrations: 2→3 adds `gps_tracks.batteryPercent`; 3→4 adds `documents.ocrText`.

## Navigation

```
Launch
  → EULA if needed → splash unless skipped → disclaimer if needed → biometric → Home

Home
  Import | All Docs | Search | Note | Checklist
  Overflow → Tags | Review | My Trail | Backup | Restore | Sample trip | Settings | Feedback | About
```

Reset App lives in Settings → Danger zone, not the overflow menu.

## My Trail

Optional foreground location service (`SafetyTrailService`). Requests fine/coarse location, FGS location, and notifications only when started. Keeps a unique place when the fix is accurate enough and far enough from the last one. Window is 24 hours. User can share Maps links; Paperstow does not upload tracks.

## Build

| Item | Pin |
|------|-----|
| AGP | 8.13.2 |
| Kotlin / Compose compiler | 2.2.21 |
| Compose BOM | 2026.06.01 |
| Hilt | 2.58 |
| Room | 2.8.5 |
| ML Kit text recognition | 16.0.1 (Latin, bundled) |
| ML Kit document scanner | 16.0.0 |
| Zip4j | 2.11.6 |
| Bouncy Castle | 1.86 |
| Kotest | 6.2.5 |
| minSdk / targetSdk / compileSdk | 26 / 36 / 36 |

SBOM: `GenerateSbomTask` writes CycloneDX 1.5 JSON into generated assets and `app/build/outputs/sbom/`.

CI: `android-actions/setup-android@v4` with `packages: platform-tools`. v3 installed the removed `tools` package and failed.

Full license inventory: [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
