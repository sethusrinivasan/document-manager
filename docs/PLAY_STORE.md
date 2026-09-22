# Play Console checklist — Paperstow 1.1.0 (versionCode 3)

## Store listing

**Title (9 / 30):** `Paperstow`

Package stays `com.app.paperstow`. Do not rename the applicationId.

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

**Full description** (paste into Play Console; no Auto, GPS, medical, expiry, Schengen, or cloud-backup claims):

```
Paperstow is where the family stows travel papers: passports, visas, boarding passes, hotel bookings, and insurance.

They live on this phone, encrypted, and stay searchable at the gate — even in airplane mode. No Paperstow account. No Paperstow servers.

Import from the camera, a single file, or a whole folder. On-device OCR reads the page, suggests a type (passport, visa, ticket, and similar), and tags it. Search by name, tag, or words from the page. If you already keep scans in folders on the phone, subfolder names become tags.

When a family member needs a copy, share through Android’s share sheet. When you change phones, archive everything as a password-protected ZIP to a folder you pick, then restore it.

• AES-256-GCM encryption; keys stay in Android KeyStore
• Unlock with this device’s fingerprint, face, or screen lock
• Search tags and extracted text
• Folder import with subfolder tags
• Password-protected ZIP archive and restore
• No ads. No document cap. Open source (Apache 2.0)

Documents never leave the phone unless you share or export them.

Digital copies do not replace originals. Paperstow is not a government or border-control app and does not give immigration advice. Check entry rules with official sources.
```

**Category:** Tools (or Productivity). Not Travel & Local — that shelf is trip planners.

## Android Auto (the current rejection)

Play rejected version code 2 because reviewers opened Android Auto and the media browser failed.

This release **removes Android Auto completely**:
- No `MediaBrowserService`
- No `com.google.android.gms.car.application` metadata
- Manifest uses `tools:node="remove"` so libraries cannot merge Auto back in
- `androidx.media` dependency removed

### You must still do this in Play Console

Google requires the compliant AAB on **every track**, not only Production.

1. Open each track that still has version code 2: Internal / Closed / Open / Production.
2. Create a new release (or edit a draft). Discard any draft that still contains the old bundle.
3. Upload `app/build/outputs/bundle/release/app-release.aab` (versionCode **3**).
4. Confirm version code 2 is under **Not included**.
5. Save → Review release → roll out to 100%.
6. Repeat for every testing track.
7. In Play Console, turn off / leave the **Android Auto** program if it is still enrolled. Do not claim Android Auto in the listing.

## Data safety form

- Location: **no**
- Photos / files: user-provided, stored on device
- Shared with third parties: **no**
- Collected: none, except optional local telemetry the user can turn on
- Encryption in transit: only if the user exports a password-protected ZIP
- ML Kit: on-device; optional model download

## Permissions (must match the binary)

Declared and used:
- Camera — Take Photo only
- Internet — optional ML Kit OCR model refresh; documents are not uploaded

Explicitly removed from the merged manifest:
- Location (fine/coarse/background)
- Foreground service / location FGS
- Wi-Fi state
- Notifications
- Android Auto (`com.google.android.gms.car.application`)
- Automotive hardware feature

Do **not** declare location, SMS, Android Auto, or media-read in Play Console.

## Privacy policy URL

In-app and Console: `https://sethusrinivasan.github.io/document-manager/privacy.html`

Enable GitHub Pages for this repo (Settings → Pages → Deploy from branch `main` / `/docs`).

## Screenshots

Use import, tags, preview, search, and settings only. Do not show Experimental Features, WiFi Share, GPS, or Android Auto.

## Release signing

Set these before `./gradlew bundleRelease`:

```
export DOCVAULT_KEYSTORE=$HOME/release.keystore
export DOCVAULT_STORE_PASSWORD=...
export DOCVAULT_KEY_ALIAS=upload_key
export DOCVAULT_KEY_PASSWORD=...
```
