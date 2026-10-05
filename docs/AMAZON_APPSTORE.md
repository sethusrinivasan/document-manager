# Amazon Appstore Checklist — Paperstow 1.1.0

This guide covers assets, metadata, and submission steps for the **Amazon Appstore**.

---

## 1. App Icons

Amazon Appstore requires two icon assets in **PNG** format with transparency (32-bit RGBA):

| Icon Type | Required Dimension | Path | Notes |
|---|---|---|---|
| **Small Icon** | **114 × 114 px** | [`docs/amazon/icon/paperstow-icon-114x114.png`](docs/amazon/icon/paperstow-icon-114x114.png) | 32-bit RGBA PNG with transparency (squircle mask) |
| Small Icon (Circle) | 114 × 114 px | [`docs/amazon/icon/paperstow-icon-114x114-circle.png`](docs/amazon/icon/paperstow-icon-114x114-circle.png) | Alternative circular mask with transparent background |
| **Large Icon** | **512 × 512 px** | [`docs/amazon/icon/paperstow-icon-512x512.png`](docs/amazon/icon/paperstow-icon-512x512.png) | 32-bit RGBA PNG with transparency |

---

## 2. Promotional Screenshots

Amazon Appstore requires a **minimum of 3 screenshots** (maximum 10) in PNG or JPEG format matching one of the approved resolutions.

We have generated **5 feature screens** across all approved resolutions in both **portrait** (full-bleed device) and **landscape** (branded showcase frame) orientations:

### Feature Screens:
1. `01_home_vault` — Home screen with sample travel folders (Passports, Visas, Tickets, Insurance)
2. `02_tag_folder` — Tag organization & categorized document cards
3. `03_document_preview` — Encrypted document preview & metadata viewer
4. `04_import_scan` — Multi-source import (PDF, camera document scanner, folder import)
5. `05_ocr_search` — On-device full-text OCR search across document contents

### Available Resolutions:

| Resolution Pair | Portrait Folder | Landscape Folder | Supported Formats |
|---|---|---|---|
| **1920 × 1080** | [`docs/amazon/screenshots/portrait/1080x1920/`](docs/amazon/screenshots/portrait/1080x1920/) | [`docs/amazon/screenshots/landscape/1920x1080/`](docs/amazon/screenshots/landscape/1920x1080/) | PNG & JPG |
| **1280 × 800** | [`docs/amazon/screenshots/portrait/800x1280/`](docs/amazon/screenshots/portrait/800x1280/) | [`docs/amazon/screenshots/landscape/1280x800/`](docs/amazon/screenshots/landscape/1280x800/) | PNG & JPG |
| **1280 × 720** | [`docs/amazon/screenshots/portrait/720x1280/`](docs/amazon/screenshots/portrait/720x1280/) | [`docs/amazon/screenshots/landscape/1280x720/`](docs/amazon/screenshots/landscape/1280x720/) | PNG & JPG |
| **1920 × 1200** | [`docs/amazon/screenshots/portrait/1200x1920/`](docs/amazon/screenshots/portrait/1200x1920/) | [`docs/amazon/screenshots/landscape/1920x1200/`](docs/amazon/screenshots/landscape/1920x1200/) | PNG & JPG |
| **2560 × 1600** | [`docs/amazon/screenshots/portrait/1600x2560/`](docs/amazon/screenshots/portrait/1600x2560/) | [`docs/amazon/screenshots/landscape/2560x1600/`](docs/amazon/screenshots/landscape/2560x1600/) | PNG & JPG |
| **1024 × 600** | [`docs/amazon/screenshots/portrait/600x1024/`](docs/amazon/screenshots/portrait/600x1024/) | [`docs/amazon/screenshots/landscape/1024x600/`](docs/amazon/screenshots/landscape/1024x600/) | PNG & JPG |
| **800 × 480** | [`docs/amazon/screenshots/portrait/480x800/`](docs/amazon/screenshots/portrait/480x800/) | [`docs/amazon/screenshots/landscape/800x480/`](docs/amazon/screenshots/landscape/800x480/) | PNG & JPG |

---

## 3. Binary Package for Amazon Appstore

- **Binary to upload:** `paperstow-*-release.apk` (built via `./gradlew assembleRelease` or downloaded from GitHub Releases)
- **App Bundle alternative:** `paperstow-*-release.aab`
- **Package Name:** `com.app.paperstow`
- **Target SDK:** 36 (Amazon Appstore requires 33+)

---

## 4. Regenerating Assets

To re-generate all Amazon Appstore icons and screenshots from source captures:

```bash
python3 scripts/generate-amazon-assets.py
```
