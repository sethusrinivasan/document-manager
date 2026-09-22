# Privacy Policy — Paperstow

**Last updated:** 2026-09-21

## Overview

Paperstow ("the App") is a local vault for travel papers on Android. Passports, visas, tickets, and similar files stay on your device. The App has no backend server and does not create an account.

## Data collection

### Optional, on-device only
- **Usage telemetry** (opt-in): anonymous feature-use counts such as "import used 5 times." No document content, file names, or personal identifiers. Stored locally. Nothing is sent unless you choose to share a report.

### What we do not collect
- Document content or files
- Personal identification information
- Location or GPS history
- Contacts or phone numbers
- Photos except those you explicitly import

## Data storage

All documents, metadata, and settings are stored **locally on your device**. Files are encrypted with AES-256-GCM using keys in Android KeyStore. The App does not transmit documents unless you use Android's share sheet or export a password-protected archive to a folder you choose.

## Permissions

| Permission | Purpose | When requested |
|-----------|---------|----------------|
| Camera | Scan a document | When you tap Take Photo |
| Internet | Optional first-run download of the on-device ML Kit OCR model | Not prompted; used only if the model is not already present |

Denying camera permission does not break file or folder import, viewing, search, or share.

## Third-party libraries

- **ML Kit Text Recognition** (on-device): OCR runs on your device. Google's ML Kit may download its recognition model on first use. Document images are not uploaded to a Paperstow server.

## Data sharing

We do not sell or share your data. If you use Share or Archive, you choose the destination app or folder. That destination is then subject to its own policies.

## Children's privacy

This App is not directed at children under 13 and does not knowingly collect data from children.

## Contact

https://github.com/sethusrinivasan/document-manager/issues
