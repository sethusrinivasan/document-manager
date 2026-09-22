# Security Policy

## Supported versions

| Version | Package | Supported |
|---------|---------|-----------|
| 1.1.x (`versionCode` 3+) | `com.app.paperstow` | Yes |
| 1.0.x GitHub Releases | older `document-manager` / `com.app.traveldocs` builds | Fixes land on current `main` only |

There is no backport branch. Install current `main` / the latest GitHub Release after the Paperstow 1.1 cut.

## What this app assumes

- Document files on the phone are encrypted with AES-256-GCM. Keys live in Android KeyStore.
- A **backup ZIP is a transport archive**: files are decrypted into the ZIP so another device can restore. A blank password writes an unencrypted ZIP. Treat that file as plaintext documents.
- My Trail points stay on the device unless you share a Maps link or include GPX in a backup.
- Optional telemetry never leaves the device unless you email a report.

## Reporting a vulnerability

Open a [private security advisory](https://github.com/sethusrinivasan/document-manager/security/advisories/new) if you can, or email the maintainer via the address on the GitHub profile. Do not file a public issue with exploit details.

You should hear back within a week. If we confirm the report, we will fix it on `main` and credit you if you want that.

Please include: affected version / commit, device or emulator API level, and steps that stay on a device you own.
