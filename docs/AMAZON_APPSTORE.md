# Amazon Appstore & Fire TV Checklist — Paperstow 1.1.0

This guide covers assets, metadata, and submission steps for the **Amazon Appstore** and **Amazon Fire TV**.

---

## 1. Amazon Appstore Icons (Tablet & Mobile)

Amazon Appstore requires two icon assets in **PNG** format with transparency (32-bit RGBA):

| Icon Type | Required Dimension | Path | Notes |
|---|---|---|---|
| **Small Icon** | **114 × 114 px** | [`docs/amazon/icon/paperstow-icon-114x114.png`](docs/amazon/icon/paperstow-icon-114x114.png) | 32-bit RGBA PNG with transparency (squircle mask) |
| Small Icon (Circle) | 114 × 114 px | [`docs/amazon/icon/paperstow-icon-114x114-circle.png`](docs/amazon/icon/paperstow-icon-114x114-circle.png) | Alternative circular mask with transparent background |
| **Large Icon** | **512 × 512 px** | [`docs/amazon/icon/paperstow-icon-512x512.png`](docs/amazon/icon/paperstow-icon-512x512.png) | 32-bit RGBA PNG with transparency |

---

## 2. Amazon Appstore Screenshots (Tablet & Mobile)

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

## 3. Amazon Fire TV Assets

Amazon Fire TV listings require dedicated widescreen 16:9 assets with **no transparency**:

| Asset Type | Dimension | Format | Transparency | Path |
|---|---|---|---|---|
| **Fire TV App Icon** | **1280 × 720 px** | **PNG** | **No transparency (24-bit RGB)** | [`docs/amazon/firetv/icon-1280x720.png`](docs/amazon/firetv/icon-1280x720.png) |
| Fire TV App Icon (Named) | 1280 × 720 px | PNG | No transparency (24-bit RGB) | [`docs/amazon/firetv/paperstow-firetv-icon-1280x720.png`](docs/amazon/firetv/paperstow-firetv-icon-1280x720.png) |
| **Screenshots (5 screens)** | **1920 × 1080 px** | **JPG & PNG** | **No transparency (24-bit RGB)** | [`docs/amazon/firetv/screenshots/`](docs/amazon/firetv/screenshots/) |
| Fire TV Background | 1920 × 1080 px | JPG & PNG | No transparency (24-bit RGB) | [`docs/amazon/firetv/background-1920x1080.png`](docs/amazon/firetv/background-1920x1080.png) |

### Fire TV Screenshots (1920 × 1080 landscape):
- `01_home_vault.png` / `01_home_vault.jpg` — Family Travel Vault & Folders
- `02_tag_folder.png` / `02_tag_folder.jpg` — Trip Tagging & Organized Documents
- `03_document_preview.png` / `03_document_preview.jpg` — Encrypted Document Viewer
- `04_import_scan.png` / `04_import_scan.jpg` — Multi-Source Import & Scanner
- `05_ocr_search.png` / `05_ocr_search.jpg` — On-Device OCR & Full-Text Search

*All Fire TV assets respect the 10-foot TV viewing experience and fit within Amazon's 882 × 448 px safe zone.*

---

## 4. Binary Package for Amazon Appstore

- **Binary to upload:** `paperstow-*-release.apk` (built via `./gradlew assembleRelease` or downloaded from GitHub Releases)
- **App Bundle alternative:** `paperstow-*-release.aab`
- **Package Name:** `com.app.paperstow`
- **Target SDK:** 36 (Amazon Appstore requires 33+)

---

## 5. Regenerating Assets

To re-generate all assets:

```bash
# Generate Amazon Appstore mobile/tablet icons and screenshots:
python3 scripts/generate-amazon-assets.py

# Generate Fire TV app icon, screenshots, and background:
python3 scripts/generate-firetv-assets.py
```
