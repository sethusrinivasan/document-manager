# Privacy Policy — Document Manager

**Last updated:** 2025-01-01

## Overview

Document Manager ("the App") is a document storage and organization app for Android. We respect your privacy and are committed to protecting your personal data.

## Data Collection

### What we collect (with your consent)
- **Anonymous usage telemetry**: Feature usage counts (e.g., "import used 5 times") to improve the app. No document content is collected. This is opt-in only.

### What we do NOT collect
- Document content or files
- Personal identification information
- Location history (GPS data stays on-device only)
- Photos or camera data
- Contacts or phone numbers

## Data Storage

All documents, metadata, and settings are stored **locally on your device only**. The App does not have a backend server and does not transmit your documents anywhere unless you explicitly use the Share, Backup, or Export features.

## Permissions

| Permission | Purpose | When Requested |
|-----------|---------|----------------|
| Camera | Scan documents using phone camera | When you tap "Take Photo" |
| Location | Tag documents with import location; GPS tracking (opt-in) | When you first import a document |
| Internet | Google Drive backup/restore, S3 backup (opt-in) | When you configure cloud backup |
| Notifications | GPS tracking indicator, crash report notification | When GPS tracking is enabled |

All permissions are requested at the time of use, not on first launch. Denying any permission does not break core app functionality.

## Third-Party Services

- **Google Drive API** (optional): Used only when you choose to backup/restore via Google Drive. Only accesses files created by this app.
- **ML Kit** (on-device): Text recognition runs entirely on your device. No data is sent to Google servers.

## Data Sharing

We do not sell, trade, or share your data with any third parties. When you use the Share feature, data is shared only with the app you choose (email, WhatsApp, etc.) via Android's standard share mechanism.

## Security

- Documents are encrypted with AES-256-GCM using keys stored in Android KeyStore (hardware-backed)
- Biometric authentication protects access to the app
- Temporary shared files are cleaned up automatically

## Children's Privacy

This app is not directed at children under 13 and does not knowingly collect data from children.

## Changes

We may update this policy. Changes will be noted in the app's About section.

## Contact

For questions about this privacy policy, contact: developer@documentmanager.app
