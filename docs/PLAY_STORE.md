# Play Console checklist — Paperstow 1.1.0 (versionCode 3)

## Store listing

**Title (9 / 30):** `Paperstow`

**Package:** `com.app.paperstow`. Do not rename `applicationId`. This id does not upgrade leftover `com.app.traveldocs` installs.

**Do not use these titles**

| Avoid | Why |
|-------|-----|
| DocVault | OPPO/ColorOS `com.os.docvault` (100K+) and many clones |
| TravelDocs | Taken |
| TravelVault | Taken (`TravelVault: Trip Organizer`) |
| Travel Document Vault | Exact competitor |
| Dokomo / Docomo | NTT Docomo impersonation / trademark |
| Offpapers | Offpaper GmbH is already on Play |

**Short description (73 / 80):**

```
Stow family passports, visas, tickets. OCR on-device. Encrypted. No cloud.
```

**Full description** (paste into Play Console; no Auto, cloud backup, medical-device, expiry, or Schengen claims):

```
Paperstow is where the family stows travel papers: passports, visas, boarding passes, hotel bookings, and insurance.

They live on this phone, encrypted, and stay searchable at the gate — even in airplane mode. No Paperstow account. No Paperstow servers.

Import from the camera, a single file, or a whole folder. On-device OCR reads the page, suggests a type (passport, visa, ticket, and similar), and tags it. Search by name, tag, or words from the page. If you already keep scans in folders on the phone, subfolder names become tags. You can also write a note or a checklist and tag it.

When a family member needs a copy, share through Android’s share sheet. When you change phones, export a ZIP archive to a folder you pick (password optional), then restore it.

Optional My Trail keeps unique places from the last 24 hours on this phone only. Locations are not uploaded.

• AES-256-GCM encryption; keys stay in Android KeyStore
• Unlock with this device’s fingerprint, face, or screen lock
• Search tags and extracted text
• Folder import with subfolder tags
• ZIP archive and restore (password optional)
• No ads. Open source (Apache 2.0)

Documents never leave the phone unless you share or export them. There is a per-member document limit in the app.

Digital copies do not replace originals. Paperstow is not a government or border-control app and does not give immigration advice. Check entry rules with official sources.
```

**Category:** Tools (or Productivity). Not Travel & Local — that shelf is trip planners.

## Android Auto (version code 2 rejection)

Play rejected version code 2 because reviewers opened Android Auto and the media browser failed.

This release **keeps Android Auto out**:

- No `MediaBrowserService`
- Manifest uses `tools:node="remove"` on `com.google.android.gms.car.application`
- `androidx.media` is not a dependency

Upload the versionCode **3** AAB on every track that still has version code 2. Leave the Android Auto program if it is still enrolled. Do not claim Android Auto in the listing.

## Data safety form

Match the **merged manifest**, not an older checklist:

| Data type | Declare |
|-----------|---------|
| Location | Yes — optional My Trail, on device only, not shared. Fine + coarse. Foreground service while the user has started the trail. Not background location. |
| Photos / files | User-provided, stored on device |
| Shared with third parties | No |
| Collected by developer | None. Optional usage counts stay on the device unless the user emails a report. |
| Encryption in transit | Only if the user exports a ZIP and then uploads that file themselves |
| ML Kit | On-device Latin OCR (bundled). Play services may refresh a model. Documents are not uploaded to a Paperstow server. |

## Permissions (must match the binary)

Declared and used:

- `CAMERA` — fallback Take Photo / scanner path; requested when that action runs
- `INTERNET` — optional ML Kit / scanner model refresh; documents are not uploaded
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` — My Trail only, when the user starts it
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` — My Trail while recording
- `POST_NOTIFICATIONS` — shown while My Trail is recording

Explicitly stripped from the merged manifest:

- `ACCESS_BACKGROUND_LOCATION`
- `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`
- Automotive hardware feature
- Android Auto application metadata

Do **not** declare SMS, media-read, or Android Auto in Play Console. Do declare location if you keep My Trail in the binary.

## Privacy policy URL

In-app and Console: `https://sethusrinivasan.github.io/document-manager/privacy.html`

Enable GitHub Pages (Settings → Pages → Deploy from branch `main` / `/docs`). Source: [PRIVACY_POLICY.md](PRIVACY_POLICY.md) and [privacy.html](privacy.html).

## Store graphics

Upload these from `docs/play/` (also copied to `app/src/main/ic_launcher-playstore.png`):

| Play Console field | File | Spec |
|--------------------|------|------|
| App icon | [docs/play/app-icon-512.png](play/app-icon-512.png) | PNG, 512×512, RGB, no alpha, 172 KB |
| Feature graphic | [docs/play/feature-graphic-1024x500.png](play/feature-graphic-1024x500.png) | PNG, 1024×500, RGB, no alpha, 151 KB |

The icon is a full-bleed square. Play applies the rounded mask — do not upload a screenshot or a file with transparent corners.

## Screenshots

Phone captures from `docs/screenshots/` (Paperstow 1.1 emulator, sample trip loaded). Prefer:

- `2.jpg` Home with sample folders
- `3.jpg` Tag folder
- `4.jpg` Document preview
- `6.jpg` Import (file / folder / scan — no Drive)
- `7.jpg` Search
- `13.jpg` Settings
- `14.jpg` About

Do not upload EULA, diagnostics, or any leftover Drive / Experimental Features shots.

## Release signing

```
export DOCVAULT_KEYSTORE=$HOME/release.keystore
export DOCVAULT_STORE_PASSWORD=...
export DOCVAULT_KEY_ALIAS=upload_key
export DOCVAULT_KEY_PASSWORD=...
./scripts/build-release.sh
```

Keystore helper: `./scripts/release_keystore.sh` → `~/release.keystore`.
