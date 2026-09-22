# Hackathon submission (historical)

This file is the original Kiro hackathon write-up. The shipping product is **Paperstow** (`com.app.paperstow`), version 1.1.0. Drive, S3, Wi-Fi share, Android Auto, URL import, and experimental-feature toggles were removed. Backup/restore is a normal feature with an **optional** ZIP password.

**Repository:** https://github.com/sethusrinivasan/document-manager  
**Releases:** https://github.com/sethusrinivasan/document-manager/releases  
**Current README:** [../README.md](../README.md)

CI uploads `document-manager-debug.apk` from `assembleDebug`. A release APK is not guaranteed on every tag.

## Demo video

https://youtube.com/shorts/E43BueHc3gQ?feature=share — recorded against the older Document Manager UI.

## What shipped for the hackathon

A privacy-first Android vault: AES-256-GCM files, KeyStore keys, on-device ML Kit OCR, no backend. Spec-first via Kiro (`.kiro/specs/travel-document-manager/`). Those specs describe the original feature set, not today's Play build.

## What Paperstow 1.1 actually includes

Import (file, folder, on-device scan), notes and checklists, tags, search over OCR text, share sheet, optional-password ZIP backup/restore, optional My Trail, dark theme, tips toggle, Reset App in Settings, About + bundled SBOM.

## How to run (current)

```bash
git clone https://github.com/sethusrinivasan/document-manager.git
cd document-manager
bash scripts/setup.sh
./gradlew assembleDebug
./scripts/deploy.sh debug
./gradlew testDebugUnitTest
```

Regeneration prompt (historical): [KIRO_GENERATION_PROMPT.md](KIRO_GENERATION_PROMPT.md).
