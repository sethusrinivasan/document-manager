# Document Manager — Technical Architecture Deep Dive

## The 50,000-foot view

```
┌─────────────────────────────────────────────────────────────────┐
│                        ANDROID DEVICE                             │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │ Presentation │  │   Domain     │  │       Data            │  │
│  │   (Compose)  │  │ (Pure Kotlin)│  │  (Room, MLKit, FS)    │  │
│  │              │  │              │  │                       │  │
│  │ Screens      │→ │ UseCases     │→ │ Repositories (Impl)   │  │
│  │ ViewModels   │  │ Models       │  │ DAOs                  │  │
│  │ Navigation   │  │ Interfaces   │  │ File Storage          │  │
│  └──────┬───────┘  └──────────────┘  │ OCR Engine            │  │
│         │                            │ NLP Parser            │  │
│  ┌──────▼───────┐                    └───────────────────────┘  │
│  │    Debug      │                                               │
│  │ Logger        │  ┌──────────────────────────────────────────┐ │
│  │ Telemetry     │  │  Android Platform Services                │ │
│  │ CrashHandler  │  │  KeyStore | Biometric | Location | BT    │ │
│  │ GPS Service   │  └──────────────────────────────────────────┘ │
│  └───────────────┘                                               │
├─────────────────────────────────────────────────────────────────┤
│  Storage: /data/data/com.app.traveldocs/                         │
│  ├── databases/traveldocs.db        (Room, unencrypted)          │
│  ├── files/docs/{member}/{id}.enc   (AES-256-GCM encrypted)     │
│  ├── files/debug_logs/              (Debug log file)             │
│  └── shared_prefs/                  (Consent, flags, contacts)   │
└─────────────────────────────────────────────────────────────────┘
```

---

The short version: documents come in through the presentation layer, get processed by
domain use cases, and end up encrypted on disk via the data layer. Everything is DI-wired
through Hilt. Debug tooling runs alongside but doesn't touch production logic.

## Layer Architecture

### 1. Domain Layer (`domain/`)
Pure Kotlin. No Android dependencies (except `android.net.Uri` in one interface).

```
domain/
├── model/          → Data classes, enums (Document, Tag, SearchResult, etc.)
├── repository/     → Interface contracts (11 interfaces)
└── usecase/        → Business orchestration (DocumentImportUseCase)
```

**Key principle**: Domain never imports from `data/` or `presentation/`. All dependencies point inward.

### 2. Data Layer (`data/`)
Implements domain interfaces. Contains all Android/third-party dependencies.

```
data/
├── local/
│   ├── dao/            → Room DAOs (SQL queries)
│   ├── entity/         → Room entities (DB schema)
│   ├── auth/           → AuthRepository, SessionManager, SecurityPreferences
│   ├── crypto/         → PinHasher (Argon2id), KeyDerivation (HKDF), DeviceKeyManager
│   └── storage/        → DocumentFileStorageImpl (AES-256-GCM)
├── scanner/            → MlKitMetadataExtractor, CameraDocumentScanner, SmartFileNamer
├── nlp/                → RegexNaturalLanguageParser, BasicDocumentChecklistGenerator
├── tags/               → AutoTagGeneratorImpl
├── drive/              → GoogleDriveImporter, GoogleDriveServiceProvider
├── importer/           → FileDocumentImporter, DocumentFormatValidator
└── backup/             → BackupManager, BackupRestore, S3BackupUploader
```

### 3. Presentation Layer (`presentation/`)
Jetpack Compose screens + Hilt ViewModels.

```
presentation/
├── MainActivity.kt         → Navigation host, screen router, biometric gate, pull-to-refresh
├── auth/                   → BiometricAuthScreen, PinEntryScreen
├── onboarding/             → DisclaimerScreen, ConsentScreen
├── documents/              → ImportScreen, DocumentListScreen, DocumentViewerScreen
│                             BatchImportViewModel, CameraImportViewModel
├── search/                 → SearchScreen, SearchViewModel
├── tags/                   → TagManagementScreen, TagManagementViewModel
├── settings/               → SettingsScreen (with experimental feature toggles)
├── backup/                 → BackupScreen (local/Drive/S3 + restore)
├── diagnostics/            → DiagnosticsScreen (system status + GPS tracking)
└── common/                 → CountryCodePicker (reusable component)
```

### 4. DI Layer (`di/`)
Hilt modules wiring interfaces to implementations.

```
di/
└── AppModule.kt → BindingsModule (@Binds) + DatabaseModule (@Provides)
```

### 5. Debug Layer (`debug/`)
Development tools, logging, telemetry. Gated on BuildConfig.DEBUG + user consent.

```
debug/
├── DebugLogger.kt              → 3-destination logger (Logcat + buffer + file)
├── DebugLogScreen.kt           → In-app log viewer UI
├── CrashHandler.kt             → Global uncaught exception handler
├── UsageTelemetry.kt           → Non-intrusive usage tracking (consent-gated)
├── SystemTelemetry.kt          → System resource snapshot (memory, battery, GPS)
├── LocationTrackingService.kt  → Foreground GPS service (configurable interval)
├── TrackingSettingsPanel.kt    → GPS tracking toggle UI
├── TempFileCleanup.kt          → Wipes shared_docs cache on pause
└── LoggingInterceptors.kt      → Utility wrappers for timed operations
```

---

## Data Flow: Document Import Pipeline

```
User picks file → ContentResolver reads bytes
       │
       ▼
ImportViewModel.importFile(uri)
       │
       ├─ Reads bytes from URI
       ├─ Detects MIME → DocumentFormat
       ├─ Computes SHA-256 hash
       ├─ Dedup check: query DB for same filename
       │     ├─ Duplicate found → Show Replace/Cancel dialog
       │     └─ No duplicate → continue
       │
       ▼
DocumentImportUseCase.importAndProcess(importedDoc, memberId)
       │
       ├─ Step 1: fileStorage.store(memberId, bytes, format)
       │     → Generates UUID as fileId
       │     → Encrypts with AES-256-GCM (key from Android KeyStore)
       │     → Writes to /files/docs/{memberId}/{fileId}.enc
       │     → Returns fileId
       │
       ├─ Step 2: metadataExtractor.extract(bytes)
       │     → ML Kit TextRecognizer → OCR text
       │     → classifyFromText() → DocumentType
       │     → extractMetadata(text, type) → Map<MetadataField, ExtractedValue>
       │     → calculateConfidence() → Float
       │     → Returns ExtractionResult (or null on failure → graceful degradation)
       │
       ├─ Step 3: autoTagGenerator.generateTags(type, metadata)
       │     → Type tag: PASSPORT→"passport", etc.
       │     → Destination tag from metadata
       │     → Date tags: "expires-2025", "issued-2020"
       │     → Returns List<String> (wrapped in try/catch → empty on failure)
       │
       └─ Step 4: documentRepository.insert(document)
             → Room transaction: insert entity + metadata + tags
             → Capacity check: max 100 docs per member
             → Returns Result<String> (document ID = fileId)

       Note: Between Step 3 and Step 4, the import pipeline captures GPS
       coordinates (LocationManager.getLastKnownLocation) and stores them
       as metadata fields IMPORT_LATITUDE and IMPORT_LONGITUDE.
```

---

## Security Architecture

```
┌─────────────────────────────────────────────────┐
│                ACCESS CONTROL                     │
├─────────────────────────────────────────────────┤
│  Biometric (fingerprint/face/device PIN)         │
│  → Uses Android BiometricPrompt API              │
│  → BIOMETRIC_STRONG | DEVICE_CREDENTIAL          │
│  → Must authenticate on every app launch         │
└──────────────────────┬──────────────────────────┘
                       │ (authenticated)
┌──────────────────────▼──────────────────────────┐
│              FILE ENCRYPTION                      │
├─────────────────────────────────────────────────┤
│  Each document file: AES-256-GCM                 │
│  Key: Android KeyStore (hardware-backed)         │
│  Alias: "travel_docs_file_encryption_key"        │
│  Storage: {fileId}.enc = [12-byte IV][ciphertext]│
│  Secure delete: overwrite with random → delete   │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│              BACKUP SECURITY                      │
├─────────────────────────────────────────────────┤
│  Backup ZIP contains ONLY encrypted .enc files   │
│  + Room DB (metadata only, no keys)              │
│  No plaintext ever leaves the device             │
│  Useless without the device's KeyStore key       │
└─────────────────────────────────────────────────┘
```

---

## Database Schema (Room)

```sql
-- documents
CREATE TABLE documents (
    id TEXT PRIMARY KEY,           -- Same as storage file ID
    memberId TEXT NOT NULL,
    type TEXT NOT NULL,            -- DocumentType enum name
    fileId TEXT NOT NULL,
    format TEXT NOT NULL,          -- PDF, JPG, PNG, VIDEO, AUDIO
    originalFileName TEXT,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    extractionConfidence REAL,
    requiresManualReview INTEGER DEFAULT 0
);

-- document_metadata
CREATE TABLE document_metadata (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    documentId TEXT NOT NULL,
    field TEXT NOT NULL,           -- MetadataField enum name
    value TEXT NOT NULL,
    confidence REAL NOT NULL
);

-- document_tags
CREATE TABLE document_tags (
    documentId TEXT NOT NULL,
    tag TEXT NOT NULL,
    isAutoGenerated INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    PRIMARY KEY (documentId, tag)
);

-- family_members
CREATE TABLE family_members (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    pinHash TEXT NOT NULL,
    pinSalt TEXT NOT NULL,
    createdAt INTEGER NOT NULL,
    failedAttempts INTEGER DEFAULT 0,
    lockedUntil INTEGER
);
```

---

## Navigation Flow

```
App Launch
    │
    ├─ eulaAccepted? NO → EulaScreen → SplashScreen → DisclaimerScreen → BiometricAuth → Main
    ├─ splashSkipped? NO → SplashScreen → DisclaimerScreen → BiometricAuth → Main
    ├─ disclaimerAccepted? NO → DisclaimerScreen → BiometricAuth → Main
    └─ YES → BiometricAuth → Main

Main Screen (document-focused)
    ├─ [Import] → ImportScreen (Single / Folder / Drive / Camera)
    ├─ [All Docs] → DocumentListScreen → DocumentViewerScreen
    ├─ [Search] → SearchScreen → DocumentViewerScreen
    └─ [⚙️ Gear Menu]
         ├─ Backup (if enabled) → BackupScreen
         ├─ Tags → TagManagementScreen
         ├─ Settings → SettingsScreen (+ experimental toggles)
         ├─ Diagnostics → DiagnosticsScreen
         └─ Reset App → Confirmation → Process.kill
```

---

## Feature Flags System

```kotlin
FeatureFlags (SharedPreferences: "feature_flags")
├── experimental: Boolean (master toggle)
├── google_drive: Boolean (Drive backup/import)
├── s3_storage: Boolean (S3-compatible backup)
├── backup_restore: Boolean (Backup menu visibility)
└── gps_tracking: Boolean (Background GPS tracking in viewer)
```

Gating rules:
- Backup menu in gear: only when `backup_restore = true`
- Drive card in BackupScreen: only when `google_drive = true`
- S3 card in BackupScreen: only when `s3_storage = true`
- All sub-toggles hidden when `experimental = false`

---

## Key Technical Decisions

| Decision | Rationale |
|----------|-----------|
| Lazy document preview | Images decoded off-thread with subsampling (>4096px). PDF pages rendered on-demand via LazyColumn. Prevents ANR on large documents. |
| Android share sheet | Uses ACTION_SEND via FileProvider. No custom share UI needed. Compatible with all Android share targets. |
| GPS on import | Captures last known location at import time. Stored as metadata fields. Shown in viewer Properties section. |
| Room without SQLCipher | Files are encrypted individually (AES-256-GCM). DB only stores metadata. SQLCipher added 14MB and 16KB alignment issues. |
| Biometric instead of custom PIN | Device biometrics are more secure, harder to bypass, and avoid custom crypto bugs |
| ML Kit on-device OCR | Offline requirement. No API keys needed. Google-maintained. |
| Hilt DI | Standard for Android. Testable. ViewModels auto-scoped. |
| Compose-only UI | Single Activity. State-driven navigation. No Fragments. |
| Property-based testing | Kotest generates random inputs to verify invariants hold for ALL cases |
| Debug logger on-device | No server needed. Viewable in-app. Pull via adb for CI analysis. |
| Feature flags in SharedPreferences | Simple. No server. Instant toggle. Persists across restarts. |

---

## Build & Dependencies

| Category | Library | Version | Purpose |
|----------|---------|---------|---------|
| UI | Compose BOM | 2024.02.00 | Material 3 UI toolkit |
| DI | Hilt | 2.50 | Dependency injection |
| DB | Room | 2.6.1 | SQLite ORM |
| OCR | ML Kit Text Recognition | 16.0.0 | On-device OCR |
| Scanner | ML Kit Document Scanner | 16.0.0-beta1 | Camera document capture |
| Crypto | BouncyCastle | 1.77 | Argon2id PIN hashing, HKDF |
| Auth | AndroidX Biometric | 1.1.0 | Fingerprint/face auth |
| Drive | Google API Services Drive | v3 | Google Drive REST API |
| Testing | Kotest Property | 5.8.0 | Property-based testing |
| Testing | MockK | 1.13.9 | Mocking framework |
| Testing | JUnit 5 | 5.10.1 | Test runner |

---

## File System Layout

```
/data/data/com.app.traveldocs/
├── databases/
│   └── traveldocs.db                    # Room database
├── files/
│   ├── docs/
│   │   └── default-member/
│   │       ├── {uuid1}.enc             # Encrypted PDF
│   │       ├── {uuid2}.enc             # Encrypted JPG
│   │       └── {uuid3}.enc             # Encrypted video
│   └── debug_logs/
│       └── traveldocs_debug.log        # Persistent debug log (5MB rotation)
├── cache/
│   └── shared_docs/                    # Temp decrypted files for external sharing
│       └── (cleaned on app pause)
└── shared_prefs/
    ├── encryption_consent.xml          # Consent + PIN hash
    ├── disclaimer_prefs.xml            # Disclaimer + telemetry consent
    ├── feature_flags.xml               # Experimental feature toggles
    ├── location_tracking_prefs.xml     # GPS interval + state
    ├── security_alert_prefs.xml        # Recovery contacts (encrypted)
    └── traveldocs_stats.xml            # Document count cache
```
