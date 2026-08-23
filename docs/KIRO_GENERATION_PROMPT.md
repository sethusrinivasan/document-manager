# Kiro Generation Prompt — Document Manager

This file contains the prompt used to generate this app with Kiro. Use it to regenerate or create a similar app from scratch.

---

## One-Shot Prompt

```
Build a production-ready Android app called "Document Manager" in Kotlin using Jetpack Compose, Hilt, and Room. Follow these specifications exactly.

## Core Mission
A privacy-first, offline-first document management app. No backend server. No cloud dependency for core features. Everything stays on device unless user explicitly acts.

## Architecture
- Clean Architecture (Domain → Data → Presentation)
- Domain layer: pure Kotlin, zero Android imports except android.net.Uri
- Data layer: Room, ML Kit, AES-256-GCM file encryption, Hilt DI
- Presentation: Jetpack Compose + Material 3, single Activity, state-driven navigation
- Package: com.app.traveldocs
- Min SDK: 26, Target SDK: 34, ABI: arm64-v8a only
- JDK 17 (Zulu), Gradle 8.5, AGP 8.2.2

## Security
- AES-256-GCM per-file encryption with Android KeyStore (alias: "travel_docs_file_encryption_key")
- File format: [12-byte IV][AES-GCM ciphertext] stored as {fileId}.enc
- Biometric authentication on every launch (BiometricPrompt, BIOMETRIC_STRONG | DEVICE_CREDENTIAL)
- Per-document optional PIN using PBKDF2-SHA256 (10K iterations) — non-recoverable by design
- Input sanitization: tags max 50 chars [a-zA-Z0-9 _-], filenames strip /\:*?"<>|
- No global PIN or password — biometrics only for app access
- FileProvider for all file sharing (never file:// URIs)
- DebugLogger writes to background thread (never blocks main thread)

## Database
- Room 2.6.1 with two separate databases:
  - traveldocs.db (main): documents, document_metadata, document_tags, family_members
  - gps_tracks.db (separate): GPS location history ONLY (prevents write contention)
- Do NOT use fallbackToDestructiveMigration() — add explicit no-op migrations
- WAL checkpoint (PRAGMA wal_checkpoint(TRUNCATE)) before any DB file operations
- DB version: 2

## First Launch Flow
EULA → Splash → Disclaimer (telemetry consent defaults ON) → Biometric Auth → Home

## Screens & Navigation

### Home Screen
- Title: "My Private Documents" (user-customizable in Settings)
- Folders organized by tags (3-column grid)
- Special folders: "Protected" (lock icon, listed first for __PIN_PROTECTED docs), "Untagged"
- Pull-to-refresh with explicit Refresh button in top bar
- Gear menu: About, Feedback (opens GitHub Issues in browser), Review & Classify, Settings, Tags, Reset App
- Experimental menu items gated by feature flags

### Import Flow
- Single file picker
- Camera (photo) — with tag prompt after capture
- Local folder (recursive optional) — each subfolder level becomes a tag
- Import from URL (downloads to temp, auto-detects format)
- Google Drive (gated behind experimental flag)
- Progress screen: shows each file being processed, failed files listed by name with reason
- Completion: green=all pass, yellow=partial, red=all failed

### Document Viewer
- Maximized preview area (weight(1f))
- PDF: lazy page rendering via LazyColumn + single-thread PdfRenderer dispatcher
- Images: async decode with subsampling >4096px, pinch-to-zoom + pan
- Properties collapsed by default ("Show Properties" button at bottom)
- Share button in top bar: ACTION_SEND with grantUriPermission to all resolvers
- PIN lock: shows lock icon until correct PIN entered
- Swipe up/down for next/previous doc in current folder

### Settings Screen
- Personalization: custom home title
- Privacy: telemetry toggle (local only, email export)
- Appearance: dark theme toggle (calls activity.recreate() for immediate effect)
- Experimental Features section with sub-toggles:
  - Google Drive Support
  - S3 Compatible Storage
  - Backup & Restore (Preview)
  - Background GPS Tracking
  - Extended Image Formats (WebP, HEIC, BMP, GIF, DICOM) — each with sub-toggle
  - Audio Playback & Android Auto
  - WiFi Document Sharing

## Stable Features (implement fully)
1. Document import (single, camera, folder, URL)
2. On-device OCR via ML Kit — graceful degradation on failure
3. Encrypted storage with AES-256-GCM
4. Tag-based organization with color picker (12-color palette)
5. Free-text and tag-based search
6. Biometric auth
7. Per-document PIN lock/unlock/change (3-step change flow with consent)
8. Document sharing via Android share sheet
9. "Protected" folder for PIN-locked docs
10. Dark theme
11. EULA with legal sections: warranties, liability, indemnification, pricing discretion, AI usage disclosure
12. Feedback → opens https://github.com/[owner]/document-manager/issues
13. Review & Classify: batch tag untagged docs, mark OCR review complete
14. Diagnostics: system status, run DB consistency check, repair, share logs as ZIP
15. Share target: register as ACTION_SEND receiver for PDF/images/video
16. Reset App: two-step confirmation, archives DB (.001-.010 rotation), detailed report, user clicks Restart

## Experimental Features (behind FeatureFlags)
1. Google Drive import
2. S3-compatible backup
3. Backup/Restore — separate BackupOnlyScreen and RestoreOnlyScreen with:
   - Backup: mandatory PIN, detailed completion report
   - Restore: Step 1 inspect manifest, Step 2 verbose per-file progress, Step 3 file verification, Step 4 metadata consistency check using raw SQLite (not Room), user chooses "Restart App" or "Go Back"
4. Background GPS tracking (separate gps_tracks.db)
5. Extended image formats (WebP/HEIC/BMP/GIF/DICOM)
6. DICOM viewer: custom parser (no library), parse DICM magic, tag-length-value elements, pixel data at 7FE0,0010, 8/12/16-bit grayscale + RGB, window/level contrast
7. Audio playback + Android Auto: MediaBrowserService, media tree by tags, audio goes to separate system tag __AUDIO
8. WiFi sharing: NanoHTTPD embedded server, token auth (random per session), full HTML UI with JS for inline rename/tag/bulk operations
9. Import from URL: GitHub blob→raw URL conversion, auto-format detection

## System Tags (__ prefix — hidden from UI, non-removable by user)
- __PIN_PROTECTED: document has per-document PIN set
- __AUDIO: audio file (MP3/M4A/etc)
- __UNSUPPORTED: unknown/unsupported file format
- Store these in document_tags table but filter from tag manager and tag chips display

## Document Formats
PDF, JPG, PNG, VIDEO, WEBP, HEIC, BMP, GIF, DICOM, AUDIO, UNKNOWN
Each needs extension mapping, MIME type mapping, viewer handling, and share handling.

## EULA Requirements
14+ sections covering: acceptance, license grant, warranty disclaimer (AS-IS), liability cap ($0), user responsibility and compliance, data/privacy, encryption notice (non-recoverable keys), indemnification, prohibited uses, termination, modifications without notice, pricing discretion, AI usage disclosure, governing law.
Per-document PIN section must state: non-recoverable, developer cannot decrypt, user must comply with laws, app developer has zero control.

## Debug & Telemetry
- DebugLogger: async file writes via single-thread executor, 3 destinations (logcat, memory buffer, file)
- File: files/debug_logs/traveldocs_debug.log (5MB rotation)
- UsageTelemetry: local-only accumulation, email export via ACTION_SEND
- CrashHandler: stores report, shows notification to send feedback
- pull-logs.sh script in scripts/ folder

## Build & Deploy
- scripts/deploy.sh [debug|release] — default release
- scripts/pull-logs.sh [output_dir] — pulls logs from device
- scripts/setup-env.sh — sets JAVA_HOME and ANDROID_HOME
- GitHub Actions: build debug + release, create GitHub Release with both APKs, bump versionCode to github.run_number

## Documentation Structure
- README.md — project goals, stable vs experimental features, build instructions, download link
- docs/HACKATHON.md — hackathon submission writeup
- docs/ARCHITECTURE.md — technical deep-dive
- docs/wireframes.md — ASCII wireframes for all screens
- docs/KNOWN_ISSUES.md — documented library limitations (Room WAL behavior, PdfRenderer threading, DocumentFile IPC cost, ML Kit first-use download)
- docs/PRIVACY_POLICY.md — Play Store compliant
- docs/KIRO_GENERATION_PROMPT.md — this file
- .kiro/specs/travel-document-manager/ — requirements.md, design.md, tasks.md

## Key Non-Obvious Design Decisions
1. Room DB and GPS tracking must be SEPARATE databases — using same DB in foreground service triggers fallbackToDestructiveMigration silently
2. WAL checkpoint (TRUNCATE) before any DB file copy/delete — prevents transaction loss
3. Backup restore REQUIRES app restart — Room's Hilt singleton holds stale connection; no way to swap DB file under live connection
4. Backup DB must have PRAGMA user_version = 2 set after copy — prevents Room treating it as wrong version
5. Per-document PIN uses PBKDF2 (deterministic) + stored salt so it works across device restore
6. TempFileCleanup must run on onResume NOT onPause — target app (WhatsApp) needs to read file while our app is in background
7. PDF rendering needs single-thread executor — PdfRenderer is NOT thread-safe
8. Share intent needs grantUriPermission to ALL resolving packages — not just the chooser
9. DebugLogger file writes MUST be async — synchronous file I/O on main thread causes ANR
10. Manual CREATE TABLE for Room-managed tables causes "invalid schema" crash — let Room manage its own DDL
```

---

## What This Prompt Would Generate

Running this prompt in Kiro would produce:
- ~120 Kotlin source files
- ~44 test files (unit + property-based)
- Full Clean Architecture implementation
- All screens described above
- Complete spec documents in `.kiro/`
- CI/CD GitHub Actions workflow
- Deploy scripts

## Iterative Refinements Made

These are patterns discovered during development that the one-shot prompt incorporates:

- **Biometrics over PIN** — custom PIN was buggy, replaced entirely with device biometrics
- **Per-file AES instead of SQLCipher** — removed SQLCipher (14MB, alignment issues), individual file encryption is safer anyway
- **Separate GPS DB** — discovered LocationTrackingService was triggering destructive migration on main DB
- **Import chooser subfolders→tags** — user specifically wanted `Personal/Travel/2025/doc.pdf` → tags "Travel" + "2025"
- **Lazy PDF rendering** — initial all-pages-upfront approach caused ANR on 50+ page PDFs
- **Share timing** — TempFileCleanup running in onPause deleted files before WhatsApp could read them
- **Backup/Restore experimental** — data integrity not fully stable; needs WAL + version management

