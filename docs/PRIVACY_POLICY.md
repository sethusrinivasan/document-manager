# Privacy Policy — Paperstow

**Last updated:** 2026-09-22

## Overview

Paperstow ("the App") is a local vault for travel papers on Android. Package name: `com.app.paperstow`. Passports, visas, tickets, notes, and similar files stay on your device. The App has no backend server and does not create an account.

## Data collection

### Optional, on-device only

- **Usage telemetry** (opt-in, off by default): anonymous feature-use counts such as "import used 5 times." No document content, file names, or personal identifiers. Stored locally. Nothing is sent unless you choose to share a report.
- **My Trail** (opt-in): if you start it, unique places from about the last 24 hours and battery level at each save stay on this device. Paperstow does not upload those points. You may share a Maps link yourself.

### What we do not collect

- Document content or files (unless you share or export them)
- An account, email, or advertising identifier for Paperstow
- Contacts or phone numbers
- Photos except those you explicitly import or assign as a tag folder image
- Location, unless you start My Trail

## Data storage

All documents, metadata, tags, settings, and My Trail points are stored **locally on your device**. Document files are encrypted with AES-256-GCM using keys in Android KeyStore.

The App does not transmit documents unless you:

- use Android's share sheet, or
- export a ZIP archive to a folder you choose (password optional; an unprotected ZIP contains readable document bytes)

## Permissions

| Permission | Purpose | When requested |
|-----------|---------|----------------|
| Camera | Fallback photo / scan if the on-device scanner is unavailable | When you use that action |
| Internet | Optional refresh of the on-device ML Kit OCR or scanner model | Not a runtime prompt; documents are not uploaded |
| Location (fine / coarse) | Optional My Trail | When you start My Trail |
| Notifications | Shown while My Trail is recording | When you start My Trail (Android 13+) |
| Foreground location service | Keeps My Trail running while you have started it | Same as My Trail |

Denying camera does not break file or folder import, viewing, search, or share. Denying location only disables My Trail.

## Third-party libraries

- **ML Kit Text Recognition** (Latin, bundled) and **ML Kit Document Scanner**: run on the device. Google Play services may download or refresh a model. Document images are not uploaded to a Paperstow server.

## Data sharing

We do not sell or share your data. If you use Share or Archive, you choose the destination app or folder. That destination is then subject to its own policies.

## Children's privacy

This App is not directed at children under 13 and does not knowingly collect data from children.

## Contact

https://github.com/sethusrinivasan/document-manager/issues
