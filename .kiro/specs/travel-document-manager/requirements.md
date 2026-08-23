# Requirements Document

## Introduction

A mobile application for families to securely store, organize, and manage travel documents on Android devices. The app provides local storage with on-device OCR scanning and metadata extraction, PIN authentication for security, and intelligent document search capabilities. All functionality works offline without any cloud dependencies.

## Glossary

- **Document Manager**: The Android application
- **User**: Family member accessing the application
- **PIN**: Personal identification number used for authentication
- **Document**: Digital representation of travel-related papers (passport, visa, ticket, etc.)
- **Metadata**: Extracted information from documents (ID number, name, expiry dates)
- **Tag**: User-assigned or automatically generated label for document categorization
- **Scanner**: AI model that processes scanned document images to extract metadata
- **Document Type**: Category of travel document (Passport, Visa, Ticket, Hotel_Booking, Health_Insurance)

## Requirements

### Requirement 1: Local Document Storage

**User Story:** As a family member, I want to store travel documents locally on my phone, so that I can access them offline during travel.

#### Acceptance Criteria

1. THE Document Manager SHALL store all documents in encrypted local storage on the device
2. WHILE the application is running, THE Document Manager SHALL maintain an index of all stored documents
3. IF local storage becomes corrupted, THEN THE Document Manager SHALL attempt recovery and notify the user of data loss
4. THE Document Manager SHALL support storing at least 100 documents per family account
5. WHERE a document is deleted by the user, THEN THE Document Manager SHALL securely erase the document data
6. THE Document Manager SHALL NOT transmit any documents or document metadata to external servers

### Requirement 2: PIN Authentication

**User Story:** As a family member, I want to protect my travel documents with PIN authentication, so that only authorized users can access them.

#### Acceptance Criteria

1. WHEN the application is launched, THE Document Manager SHALL prompt for a PIN
2. IF an incorrect PIN is entered three times, THEN THE Document Manager SHALL lock access for 5 minutes
3. IF five consecutive incorrect PIN attempts occur, THEN THE Document Manager SHALL wipe all stored documents and require re-enrollment
4. WHILE authenticated, THE Document Manager SHALL maintain session state for up to 30 minutes of inactivity
5. WHERE a user logs out, THEN THE Document Manager SHALL clear the session and require re-authentication
6. THE Document Manager SHALL store PIN hashes using industry-standard encryption (not plaintext)

### Requirement 3: Document Import

**User Story:** As a family member, I want to import travel documents from Google Drive or via phone camera scanning, so that I can digitize my travel papers easily.

#### Acceptance Criteria

1. WHEN the user selects Google Drive import, THE Document Manager SHALL display a file picker for Google Drive documents
2. WHEN a document is selected from Google Drive, THE Document Manager SHALL download and store it locally
3. WHEN the camera scan option is selected, THE Document Manager SHALL activate the device camera for document capture
4. WHILE scanning with the camera, THE Document Manager SHALL provide visual guidance for proper document positioning
5. IF document scanning fails, THEN THE Document Manager SHALL return a descriptive error message
6. THE Document Manager SHALL support importing documents in PDF, JPG, and PNG formats
7. WHERE a document is imported, THEN THE Document Manager SHALL attempt automatic metadata extraction

### Requirement 4: AI-Powered Metadata Extraction

**User Story:** As a family member, I want the application to automatically extract metadata from scanned documents, so that I don't need to manually enter dates and identification numbers.

#### Acceptance Criteria

1. WHEN a document image is processed, THE Scanner SHALL extract document type classification
2. WHEN a passport image is processed, THE Scanner SHALL extract ID number, holder name, and expiry date
3. WHEN a visa image is processed, THE Scanner SHALL extract visa number, issue date, and expiry date
4. WHEN a ticket image is processed, THE Scanner SHALL extract booking reference, flight details, and dates
5. WHEN a hotel booking image is processed, THE Scanner SHALL extract booking reference, dates, and hotel name
6. WHEN a health insurance document is processed, THE Scanner SHALL extract policy number and coverage period
7. IF metadata extraction confidence is below 80%, THEN THE Scanner SHALL flag the document for manual review
8. THE Scanner SHALL run locally on the device using lightweight AI models
9. WHERE no extraction model exists for a document type, THEN THE Document Manager SHALL skip extraction and prompt manual entry

### Requirement 5: Automatic Tag Generation

**User Story:** As a family member, I want the application to automatically generate tags for documents, so that I can easily find them later.

#### Acceptance Criteria

1. WHEN a document is imported, THE Document Manager SHALL automatically generate tags based on document metadata
2. WHERE document type is Passport, THE Document Manager SHALL generate the tag "passport" automatically
3. WHERE document type is Visa, THE Document Manager SHALL generate the tag "visa" automatically
4. WHERE document type is Ticket, THE Document Manager SHALL generate the tag "ticket" automatically
5. WHERE document type is Hotel_Booking, THE Document Manager SHALL generate the tag "accommodation" automatically
6. WHERE document type is Health_Insurance, THE Document Manager SHALL generate the tag "health" automatically
7. WHERE extraction yields destination information, THE Document Manager SHALL generate a destination tag
8. WHERE extraction yields travel dates, THE Document Manager SHALL generate date-range tags
9. IF tag generation fails for technical reasons, THEN THE Document Manager SHALL fail silently and continue processing the document without the missing tags

### Requirement 6: User-Created Tags

**User Story:** As a family member, I want to create custom tags for documents, so that I can organize them according to my family's needs.

#### Acceptance Criteria

1. WHEN the user selects a document, THE Document Manager SHALL display a tag management interface
2. WHEN the user adds a new tag, THE Document Manager SHALL append it to the document's tag list
3. WHEN the user removes a tag, THE Document Manager SHALL remove it from the document's tag list
4. WHERE a user attempts to add a duplicate tag, THEN THE Document Manager SHALL prevent duplication
5. THE Document Manager SHALL support at least 20 tags per document
6. WHERE a tag is deleted by the user, THE Document Manager SHALL remove the tag from all associated documents

### Requirement 7: Advanced Search Functionality

**User Story:** As a family member, I want to search documents using multiple methods, so that I can quickly find what I need.

#### Acceptance Criteria

1. WHEN the user selects tag search, THE Document Manager SHALL filter documents by selected tags
2. WHEN the user enters free-form search text, THE Document Manager SHALL search document metadata and tags
3. WHEN the user enters a natural language query, THE Document Manager SHALL parse the query and return relevant documents
4. WHERE multiple search criteria are provided, THE Document Manager SHALL apply all criteria (AND logic)
5. IF no documents match the search, THEN THE Document Manager SHALL return an empty result without showing any error or informational messages
6. THE Document Manager SHALL return search results within 2 seconds for searches of 100 documents
7. THE Natural_Language_Parser SHALL understand queries about document needs based on travel context

### Requirement 8: Natural Language Search for Document Needs

**User Story:** As a family member, I want to ask the application what documents I need for a trip, so that I don't forget anything important.

#### Acceptance Criteria

1. WHEN a natural language query describes travel context, THE Natural_Language_Parser SHALL identify travel parameters (family size, origin, destination, duration)
2. WHERE a query mentions "family of 4", THE Natural_Language_Parser SHALL identify 4 travelers
3. WHERE a query mentions "living in US", THE Natural_Language_Parser SHALL identify US as origin country
4. WHERE a query mentions "Singapore", THE Natural_Language_Parser SHALL identify Singapore as destination country
5. WHERE a query mentions "week", THE Natural_Language_Parser SHALL identify 7-day duration
6. WHEN travel parameters are identified, THE Document_Generator SHALL return required document checklist based on origin and destination
7. THE Document_Generator SHALL consider family size when generating document requirements (e.g., 4 passports for 4 family members)
8. WHERE a query lacks sufficient detail, THEN THE Natural_Language_Parser SHALL prompt the user for missing information

### Requirement 9: Missing Document Detection

**User Story:** As a family member, I want the application to identify missing required documents for my trip, so that I can prepare everything in advance.

#### Acceptance Criteria

1. WHEN travel context is identified, THE Document_Generator SHALL compare existing documents against required document checklist
2. WHERE required documents are missing, THE Document Manager SHALL highlight them in the interface
3. WHERE a passport is required but missing AND the trip requires a passport, THEN THE Document_Generator SHALL suggest "Ensure passport validity is 6+ months"
4. WHERE a visa is required but missing AND the trip requires a visa, THEN THE Document_Generator SHALL suggest "Apply for visa at embassy"
5. WHERE health documentation is required but missing AND the trip requires health documentation, THEN THE Document_Generator SHALL suggest "Check vaccination requirements"
6. THE Document_Generator SHALL update missing document indicators in real-time as documents are added or removed

### Requirement 10: User-Friendly Interface

**User Story:** As a family member, I want an intuitive interface, so that all family members can use the application without technical training.

#### Acceptance Criteria

1. WHEN a new user launches the application, THE Document Manager SHALL present a guided onboarding flow
2. WHERE a user needs help, THE Document Manager SHALL provide contextual help tooltips
3. WHEN importing documents, THE Document Manager SHALL display progress indicators
4. IF an error occurs during document processing, THEN THE Document Manager SHALL provide actionable guidance
5. THE Document Manager SHALL support at least three family member accounts on a single device
6. THE Document Manager SHALL support unlimited family member accounts for families larger than 3 members
7. WHERE multiple family members use the app, THE Document Manager SHALL maintain separate document collections per member

### Requirement 11: Offline Functionality

**User Story:** As a family member traveling internationally, I want the application to work without internet access, so that I can access my documents anywhere.

#### Acceptance Criteria

1. WHILE offline, THE Document Manager SHALL allow full document browsing functionality
2. WHILE offline, THE Document Manager SHALL allow new document scanning and import
3. WHILE offline, THE Document Manager SHALL allow tag management and search operations
4. IF offline mode cannot be determined, THEN THE Document Manager SHALL assume offline capability
5. THE Document Manager SHALL NOT require internet connection for any core functionality
6. WHEN internet connectivity is restored, THE Document Manager MAY notify the user but SHALL NOT require immediate action
### Requirement 12: Document Deduplication

**User Story:** As a family member, I want the application to detect duplicate documents during import, so that I don't accidentally store the same document twice.

#### Acceptance Criteria

1. WHEN a document is imported, THE Document Manager SHALL compute a SHA-256 hash of the file content
2. WHERE a document with the same filename AND content hash already exists, THEN THE Document Manager SHALL prompt the user with a duplicate detection dialog
3. WHEN a duplicate is detected, THE Document Manager SHALL offer the user two options: "Replace existing" or "Cancel import"
4. IF the user selects "Replace", THEN THE Document Manager SHALL delete the existing document and store the new one
5. IF the user selects "Cancel", THEN THE Document Manager SHALL discard the import and return to the previous screen
6. THE Document Manager SHALL display the existing document's metadata in the duplicate dialog for user reference

### Requirement 13: Document List and Viewer

**User Story:** As a family member, I want to browse my stored documents and view them on my phone, so that I can quickly reference travel papers.

#### Acceptance Criteria

1. FROM the main screen, THE Document Manager SHALL provide access to a document list view
2. THE document list SHALL display each document's type, filename, import date, and tags
3. WHEN the user taps a document in the list, THE Document Manager SHALL render the document for viewing
4. WHERE the document is a PDF, THE Document Manager SHALL display it using lazy page-by-page rendering (LazyColumn with PdfRenderer) for responsiveness with large documents
5. WHERE the document is a JPG or PNG, THE Document Manager SHALL decode it asynchronously off the main thread (with subsampling for images >4096px) and display as a zoomable image
6. THE document list SHALL support sorting by date (newest first)
7. WHEN the document list is empty, THE Document Manager SHALL display an empty state with guidance to import

### Requirement 14: Background GPS Tracking Service

**User Story:** As a family member traveling, I want the application to log my travel route in the background, so that I have a record of my journey for reference.

#### Acceptance Criteria

1. WHEN the user enables background GPS tracking, THE Document Manager SHALL start a foreground service with a visible notification
2. WHILE the foreground service is running, THE Document Manager SHALL log GPS coordinates at the configured interval
3. WHERE the device has not moved more than 10 meters since the last log, THE Document Manager SHALL skip logging to save battery
4. THE user SHALL be able to configure the logging interval (30 seconds, 1 minute, 5 minutes, 15 minutes, 30 minutes)
5. THE default logging interval SHALL be 1 minute
6. WHEN the user disables tracking OR dismisses the notification, THE Document Manager SHALL stop the foreground service
7. THE foreground service notification SHALL clearly indicate that location is being tracked

### Requirement 15: System Telemetry Dashboard

**User Story:** As a user, I want to see system status at a glance on the main screen, so that I know my device's condition while traveling.

#### Acceptance Criteria

1. THE main screen SHALL display current battery percentage and charging status
2. THE main screen SHALL display the last known GPS coordinates with accuracy
3. THE main screen SHALL display network connectivity status (WiFi/Cellular/Offline)
4. THE main screen SHALL display the total number of documents managed
5. THE system telemetry SHALL refresh automatically every 5 seconds
6. WHERE GPS permission is not granted, THE Document Manager SHALL display a placeholder and request permission

### Requirement 16: Debug Logging and In-App Log Viewer

**User Story:** As a developer testing the application, I want comprehensive debug logs viewable from within the app, so that I can diagnose issues during manual testing.

#### Acceptance Criteria

1. THE Document Manager SHALL log all significant operations (import, auth, search, storage, session events) with timestamps and component tags
2. THE Document Manager SHALL write logs to three destinations: Android Logcat, in-memory ring buffer (500 entries), and a persistent file on device
3. THE log file SHALL be located at a known path accessible via `adb shell run-as`
4. THE main screen SHALL provide a floating debug button that opens the in-app log viewer
5. THE in-app log viewer SHALL display color-coded log entries (debug=blue, info=green, warn=yellow, error=red)
6. THE in-app log viewer SHALL auto-scroll to the newest entry
7. THE in-app log viewer SHALL provide a "Clear logs" action
8. ON app launch and resume, THE Document Manager SHALL log system resource snapshot (memory, battery, connectivity, GPS, top processes)

### Requirement 17: Crash Tracking

**User Story:** As a developer, I want app crashes to be captured in debug logs automatically, so that I can diagnose crashes that occur during manual testing.

#### Acceptance Criteria

1. THE Document Manager SHALL install a global uncaught exception handler on application startup
2. WHEN an unhandled exception occurs, THE crash handler SHALL log the exception class, message, and full stack trace to the debug log file
3. WHEN an unhandled exception occurs, THE crash handler SHALL log the causal chain (up to 5 levels)
4. AFTER logging the crash, THE crash handler SHALL delegate to the default Android crash handler (system crash dialog)
5. THE crash log entries SHALL be persisted to the debug log file and available via `adb shell run-as` even after app restart

### Requirement 18: Tag Management

**User Story:** As a family member, I want a dedicated screen to manage all my tags (create, rename, delete), so that I can keep my document organization clean and consistent.

#### Acceptance Criteria

1. THE Document Manager SHALL provide a Tag Management screen accessible from the main dashboard
2. THE Tag Management screen SHALL list all tags with the count of documents using each tag
3. WHEN the user creates a new tag, THE Document Manager SHALL add it to the global tag list (available for later assignment)
4. WHEN the user renames a tag, THE Document Manager SHALL update the tag name across all documents that use it
5. WHEN the user attempts to delete a tag that is still assigned to documents, THE Document Manager SHALL display a warning showing the number of affected documents and require explicit confirmation
6. IF the user confirms deletion of a referenced tag, THEN THE Document Manager SHALL remove it from all associated documents before deleting
7. IF the user cancels deletion, THEN THE Document Manager SHALL retain the tag unchanged
8. THE user SHALL be able to add custom tags to any document from the document viewer screen
9. THE user SHALL be able to disassociate (remove) any tag from a document without deleting the tag globally
10. THE Tag Management screen SHALL support sorting tags alphabetically or by usage count

### Requirement 19: Security Hardening

**User Story:** As a user storing sensitive travel documents, I want the application to protect my data with defense-in-depth security measures, so that my documents remain confidential even if the device is compromised.

#### Acceptance Criteria

1. THE Document Manager SHALL NOT store documents in an unencrypted Room database in production (SQLCipher MUST be enabled)
2. THE Document Manager SHALL enforce PIN authentication before granting access to any document operations
3. THE Document Manager SHALL NOT hardcode a default member ID — member ID must be derived from authenticated session
4. THE debug log file SHALL NOT contain raw PIN values, encryption keys, or full document content
5. WHERE temporary decrypted files are shared via FileProvider, THE Document Manager SHALL delete them from the cache directory within 60 seconds or on app pause
6. THE DeviceKeyManager SHALL NOT use a fixed/zero IV for key derivation — each derivation SHALL use a unique random nonce
7. THE Document Manager SHALL enable network security configuration to block cleartext HTTP traffic
8. THE Document Manager SHALL sanitize all user input (tag names, filenames) to prevent path traversal or injection
9. THE Document Manager SHALL clear sensitive data from memory (PIN, decryption keys) after use using explicit zeroing
10. THE GPS location data in debug logs SHALL be redactable (user can disable location logging independently of tracking)
11. WHERE the app runs in debug mode (BuildConfig.DEBUG), extensive logging is permitted; in release builds, THE Document Manager SHALL disable file-based debug logging and restrict Logcat output

### Requirement 20: Encryption Consent and Region-Based Storage

**User Story:** As a user, I want to choose whether my documents are stored with encryption based on my country's regulations, so that I comply with local laws while maintaining security where permitted.

#### Acceptance Criteria

1. ON first launch, THE Document Manager SHALL display a consent screen before any other functionality
2. THE consent screen SHALL explain that encrypted storage protects documents with a PIN and that documents cannot be accessed without it
3. THE consent screen SHALL require the user to select their country/region from a list
4. WHERE the selected country permits encryption for personal use, THE Document Manager SHALL offer encrypted storage as the default option
5. WHERE the selected country restricts encryption, THE Document Manager SHALL offer unencrypted storage only and explain why
6. THE user SHALL explicitly agree to the storage terms before proceeding (checkbox + "I Agree" button)
7. IF encrypted storage is chosen, THE consent screen SHALL display a prominent warning: "Your PIN cannot be recovered. If you forget your PIN, your documents will be permanently inaccessible. Please write down and securely store your PIN."
8. THE user SHALL acknowledge the PIN irrecoverability warning separately before encryption is enabled
9. THE Document Manager SHALL store the user's consent choice and region persistently (SharedPreferences)
10. THE Document Manager SHALL NOT allow changing from encrypted to unencrypted storage after documents have been stored (irreversible choice once data exists)
11. WHERE unencrypted storage is chosen, THE Document Manager SHALL still store documents locally but without PIN protection or file encryption

### Requirement 21: Failed PIN Attempt Notifications

**User Story:** As a user, I want to be notified via SMS when someone triggers a PIN lockout on my device, so that I know if an unauthorized person is trying to access my documents.

#### Acceptance Criteria

1. ON first launch (during onboarding), THE Document Manager SHALL request a recovery phone number for SMS alerts
2. ON first launch, THE Document Manager SHALL request a recovery email address for notifications
3. WHEN a PIN lockout is triggered (3 consecutive failures), THE Document Manager SHALL send a single SMS to the configured phone number with: "Document Manager alert: PIN failed 3 times at [time] on [date]"
4. THE Document Manager SHALL NOT send SMS on every individual failed attempt (only on lockout events)
5. AFTER the lockout period expires, THE user SHALL be allowed to retry PIN entry (not permanently locked until 5 failures/wipe threshold)
6. THE user SHALL be able to view and update their recovery phone number and email from app Settings
7. THE Document Manager SHALL request SEND_SMS permission at runtime before attempting to send
8. IF SMS permission is denied, THE Document Manager SHALL log the alert locally and show an in-app notification instead
9. THE configured phone number and email SHALL be stored in encrypted SharedPreferences
10. THE SMS alert SHALL include approximate device location if GPS permission is granted

### Requirement 22: Batch Import from Google Drive Folder

**User Story:** As a family member, I want to point the app at a Google Drive folder and import all documents in it (including subfolders), so that I can bulk-import travel documents I've already collected.

#### Acceptance Criteria

1. THE Document Manager SHALL allow the user to browse their Google Drive folder structure
2. THE user SHALL be able to navigate into subfolders and select a specific folder for import
3. WHEN a folder is selected, THE Document Manager SHALL list all supported files (PDF, JPG, PNG, video) in the folder and subfolders
4. THE Document Manager SHALL display a progress bar showing: current file / total files, percentage complete, and current filename being imported
5. THE user SHALL be able to cancel the batch import at any time (already-imported files remain)
6. EACH imported file SHALL go through the standard import pipeline (store → OCR extract → auto-tag → save)
7. WHERE a file fails to import, THE Document Manager SHALL log the error, skip the file, and continue with the next
8. AFTER batch import completes, THE Document Manager SHALL display a summary: N imported, M skipped/failed
9. THE Document Manager SHALL authenticate with Google Drive using the device's Google account (Google Sign-In)
10. THE Document Manager SHALL only request read-only access to Drive (drive.readonly scope)

### Requirement 23: Batch Import from Local Phone Folder

**User Story:** As a family member, I want to select a local folder on my phone and import all documents in it, so that I can bulk-import files stored locally.

#### Acceptance Criteria

1. THE Document Manager SHALL allow the user to browse local device folders using the system document picker (ACTION_OPEN_DOCUMENT_TREE)
8. AFTER folder selection, THE Document Manager SHALL prompt the user whether to scan subfolders recursively or import only root-level files
9. WHERE subfolders are included, THE Document Manager SHALL use EACH subfolder level in the relative path as a separate auto-generated tag (e.g. root/Travel/2025/doc.pdf → tags "Travel" and "2025")
10. THE subfolder-derived tags SHALL be attached to the document after successful import alongside any OCR-generated tags
11. FILES at the root level of the selected folder SHALL receive no folder-derived tags
2. WHEN a local folder is selected, THE Document Manager SHALL enumerate all supported files (PDF, JPG, PNG, video) in the folder tree
3. THE Document Manager SHALL display the same progress UX as Drive import (progress bar, current/total, cancel button)
4. EACH imported file SHALL go through the standard import pipeline
5. WHERE a file fails, THE Document Manager SHALL skip and continue
6. AFTER completion, THE Document Manager SHALL display import summary
7. THE Document Manager SHALL NOT require MANAGE_EXTERNAL_STORAGE permission (use SAF/DocumentTree URI)

### Requirement 24: Camera Document Capture with Smart Naming

**User Story:** As a family member, I want to take a photo of a document using my phone camera, with guidance for clear capture and automatic intelligent naming, so that I can quickly digitize physical documents without manual effort.

#### Acceptance Criteria

1. THE import screen SHALL offer a "Take Photo" option alongside single file, local folder, and Drive folder
2. WHEN the user selects camera capture, THE Document Manager SHALL launch the ML Kit Document Scanner which provides edge detection, perspective correction, and auto-enhancement
3. AFTER capture, THE Document Manager SHALL display a preview of the scanned image with options to Accept or Retake
4. THE Document Manager SHALL run quick OCR text recognition on the captured image to detect document context
5. BASED on detected text, THE Document Manager SHALL generate an intelligent filename (e.g., "passport_john_smith.jpg", "visa_singapore_2025.jpg", "ticket_aa1234.jpg")
6. WHERE OCR cannot determine meaningful context, THE Document Manager SHALL fall back to naming by document type and timestamp (e.g., "document_20250810_143022.jpg")
7. THE user SHALL be able to edit the suggested filename before confirming the import
8. AFTER the user confirms, THE document SHALL proceed through the standard import pipeline (store → extract → tag → save)
9. THE camera capture SHALL use ML Kit Document Scanner in FULL mode for best quality edge detection and perspective correction
10. THE Document Manager SHALL request CAMERA permission at runtime before launching the scanner

### Requirement 25: UX Standards Compliance

**User Story:** As a user, I want the app to follow Android Material Design 3 guidelines with proper contrast, touch targets, and dark theme support, so that the app is usable in all lighting conditions and accessible to all users.

#### Acceptance Criteria

1. THE Document Manager SHALL support both light and dark color schemes using Material 3 dynamic color theming
2. ALL text and icons SHALL meet WCAG AA contrast ratios (4.5:1 for text, 3:1 for large text and icons)
3. ALL interactive elements SHALL have a minimum touch target size of 48dp x 48dp
4. ALL icons SHALL use Material Icons from `androidx.compose.material.icons` (Apache 2.0, no copyright issues)
5. ALL icons SHALL have meaningful contentDescription for screen reader accessibility
6. THE app SHALL support predictive back gesture (Android 14+)
7. THE app SHALL use Material 3 TopAppBar, Cards, Buttons, and Dialogs consistently
8. WHERE text appears on colored backgrounds, THE contrast ratio SHALL be verified for readability in low-light conditions
9. THE delete/destructive actions SHALL use red (Error color) consistently
10. THE primary actions SHALL use the app's primary color (Blue #1565C0) consistently

### Requirement 26: Quick Share to External Apps

**User Story:** As a family member, I want to select one or more documents and quickly share them via email, WhatsApp, or any other app on my phone, so that I can send travel documents to others without leaving the app.

#### Acceptance Criteria

1. FROM the document list in selection mode, THE Document Manager SHALL offer a "Share" action for selected documents
2. FROM the document viewer top app bar, THE Document Manager SHALL display a Share icon (📤) to share the current document via Android share sheet
3. WHEN sharing is triggered, THE Document Manager SHALL decrypt the selected file(s) to a temporary cache location
4. THE Document Manager SHALL launch the Android system share sheet (ACTION_SEND or ACTION_SEND_MULTIPLE) with the decrypted file(s) attached
5. THE share intent SHALL include the correct MIME type for each file (application/pdf, image/jpeg, image/png, video/*)
6. WHERE multiple documents are selected, THE Document Manager SHALL use ACTION_SEND_MULTIPLE with an ArrayList of URIs
7. THE shared files SHALL be provided via FileProvider (no direct file:// URIs)
8. THE temporary decrypted files SHALL be cleaned up on Activity onPause (existing TempFileCleanup)
9. THE share sheet SHALL show all compatible apps (Email, WhatsApp, Telegram, Messages, etc.)
10. THE Document Manager SHALL log the share action to debug telemetry (feature usage tracking)

### Requirement 29: Cloud Backup to Google Drive and AWS S3

**User Story:** As a user, I want to back up my encrypted documents to Google Drive or an AWS S3-compatible endpoint, so that I can recover my data if my phone is lost or damaged.

#### Acceptance Criteria

1. THE Document Manager SHALL offer a "Backup" option accessible from Settings
2. THE user SHALL be able to choose between Google Drive or AWS S3-compatible storage as the backup destination
3. FOR Google Drive backup, THE app SHALL authenticate via the device's Google account and request Drive file write scope
4. FOR AWS S3 backup, THE user SHALL configure: endpoint URL, bucket name, access key ID, and secret access key
5. WHEN backup is triggered, THE Document Manager SHALL package all encrypted document files + encrypted database into a single backup archive
6. THE backup archive SHALL remain encrypted (the actual file encryption is preserved — no plaintext is uploaded)
7. THE Document Manager SHALL display backup progress (file count, percentage, current file)
8. THE user SHALL be able to cancel backup at any time
9. AFTER backup completes, THE Document Manager SHALL log the timestamp, file count, and total size
10. THE Document Manager SHALL support scheduled automatic backups (daily, weekly, manual-only)
11. IF required permissions (Google Drive scope or network) are not granted, THE app SHALL prompt the user on launch with a non-blocking banner
12. THE user SHALL be able to restore from a backup (select backup file from Drive or S3, download, decrypt, import)
13. ALL backup operations SHALL be logged in debug telemetry (start, progress, complete, errors)
14. THE backup SHALL include a manifest file listing: backup timestamp, document count, app version, and member ID
15. THE Document Manager SHALL NOT upload any plaintext document content — only pre-encrypted files

### Requirement 30: Launch Disclaimer and Telemetry Consent

**User Story:** As a user, I want to be informed about the app's limitations and data practices on first launch, so that I can make an informed decision about using it.

#### Acceptance Criteria

1. ON first launch (before consent screen), THE Document Manager SHALL display a disclaimer screen
2. THE disclaimer SHALL state that the app is provided "as-is" with no warranties or additional support
3. THE disclaimer SHALL inform users that certain actions (sharing, backup) can move documents off their phone and caution is required
4. THE disclaimer SHALL state that the app has no intent to collect user content
5. THE disclaimer SHALL explain that anonymous usage telemetry may be collected (with consent) to improve the app experience
6. THE user SHALL explicitly consent to telemetry collection via a separate toggle/checkbox (opt-in, not opt-out)
7. THE user SHALL tap "I Understand & Continue" to proceed past the disclaimer
8. THE Document Manager SHALL store the disclaimer acceptance timestamp
9. IF telemetry consent is denied, THE Document Manager SHALL disable all UsageTelemetry logging (no data collected)
10. THE user SHALL be able to change telemetry consent later in Settings
11. THE disclaimer SHALL be displayed only once (not on subsequent launches)

### Requirement 31: Local Folder Backup

**User Story:** As a user, I want to back up my encrypted documents to a local folder on my phone (e.g., Downloads), so that I have a backup without needing cloud services.

#### Acceptance Criteria

1. THE backup destination options SHALL include: Local Folder, Google Drive, and AWS S3-compatible
2. FOR local backup, THE Document Manager SHALL prompt the user to select or create a folder using the system folder picker (ACTION_OPEN_DOCUMENT_TREE)
3. THE Document Manager SHALL pre-populate a suggested folder name: "TravelDocs_Backup"
4. IF the suggested folder does not exist, THE app SHALL offer to create it
5. THE backup SHALL write the encrypted ZIP archive to the selected local folder
6. THE user SHALL be able to restore from a local backup by selecting the ZIP file

### Requirement 32: Airplane Mode Status in Diagnostics

**User Story:** As a traveler, I want to see if airplane mode is on or off in the system status, so I know my connectivity state at a glance.

#### Acceptance Criteria

1. THE Diagnostics screen SHALL display airplane mode status (On/Off)
2. THE status SHALL update in real-time when airplane mode changes

### Requirement 33: Bluetooth Peer-to-Peer Document Sharing

**User Story:** As a family member, I want to pair with another phone via Bluetooth and share/copy documents directly without internet, so that I can transfer documents while traveling offline.

#### Acceptance Criteria

1. THE Document Manager SHALL offer a "Nearby Share" option that uses Bluetooth for peer-to-peer transfer
2. THE sender SHALL be able to select one or more documents to send
3. THE receiver phone SHALL also have the Document Manager installed
4. THE transfer SHALL use Bluetooth (BLE or Classic) for discovery and data transfer
5. BEFORE transfer, BOTH devices SHALL display a pairing confirmation with a matching code
6. THE transferred documents SHALL be encrypted during transit
7. THE Document Manager SHALL display transfer progress (file count, percentage)
8. THE app SHALL request BLUETOOTH_CONNECT and BLUETOOTH_SCAN permissions at runtime
9. AFTER successful transfer, THE receiving device SHALL import the documents through the standard pipeline

### Requirement 34: Adaptive GPS Tracking with Timeline Construction

**User Story:** As a traveler, I want background GPS tracking that adapts its frequency to my movement pattern, so that it captures my travel timeline without excessive battery drain.

#### Acceptance Criteria

1. BACKGROUND GPS tracking SHALL be enabled by default after consent
2. THE adaptive algorithm SHALL increase frequency when movement is detected (every 30s when moving)
3. THE adaptive algorithm SHALL decrease frequency when stationary (every 5 minutes when idle for > 2 minutes)
4. THE algorithm SHALL detect movement by comparing consecutive GPS fixes (> 20m displacement = moving)
5. THE tracking data SHALL be structured to reconstruct travel timelines (sorted timestamps with coordinates)
6. THE Document Manager SHALL store GPS history in a local database table for timeline queries
7. THE user SHALL be able to view their travel timeline as a chronological list of locations with timestamps

### Requirement 35: GPS Coordinates on Document Import

**User Story:** As a user, I want the app to automatically record my GPS location when I import a document, so I know where I was when I added each document.

#### Acceptance Criteria

1. WHEN a document is imported, THE Document Manager SHALL capture the current GPS coordinates
2. THE GPS coordinates SHALL be stored as document metadata (latitude, longitude)
3. THE document viewer SHALL display the import location coordinates in the properties section
4. IF GPS is unavailable at import time, THE field SHALL show "Location unavailable"

### Requirement 36: Advanced Backup Management

**User Story:** As a user, I want to manage multiple backups with manifests, inspect backup contents, and restore with merge/replace options, so that I have full control over my backup history.

#### Acceptance Criteria

1. EACH backup SHALL be stored in a timestamped subfolder (e.g., "TravelDocs_Backup/2024-08-15_093022/")
2. EACH backup SHALL include a manifest.json with: timestamp, document count, total size, app version, member ID, and list of file names with types
3. MULTIPLE backups to the same parent folder SHALL NOT conflict (separate subfolders)
4. THE user SHALL be able to inspect any backup: view manifest summary, document count by type, total size
5. THE user SHALL be able to preview individual files within a backup without full restore
6. ON restore, THE user SHALL choose: "Replace all" (wipe current + restore) OR "Merge (skip duplicates)"
7. WHERE "Merge" is chosen, THE app SHALL compare file names and skip already-existing documents
8. THE restore screen SHALL show progress and a summary of: restored count, skipped count, replaced count

### Requirement 37: Experimental Feature Toggles

**User Story:** As a user, I want to opt-in to experimental features that are still in development, so that I can try new functionality while understanding it may be unstable.

#### Acceptance Criteria

1. THE Settings screen SHALL include an "Experimental Features" section with a master toggle
2. WHEN the master toggle is OFF, NO experimental features SHALL be visible in the app
3. WHEN the master toggle is ON, sub-toggles SHALL appear for: Google Drive Support, S3 Compatible Storage, Backup & Restore
4. EACH sub-toggle SHALL independently control visibility of its respective feature
5. THE feature flag state SHALL persist across app restarts (SharedPreferences)
6. THE default state for all flags SHALL be OFF (stable-only experience by default)


### Requirement 38: Document Viewer Performance and UX

**User Story:** As a user viewing documents, I want the preview to be responsive and maximize screen real estate, so that I can quickly review documents without lag or clutter.

#### Acceptance Criteria

1. THE document viewer SHALL maximize the preview area by using all available screen space (weight-based layout)
2. THE document properties (tags, metadata, file location) SHALL be hidden by default behind a "Show Properties" button at the bottom
3. WHEN the user taps "Show Properties", THE viewer SHALL expand to show tags and metadata in a scrollable section
4. WHERE a PDF has more than 5 pages, THE viewer SHALL render pages lazily (only visible pages in memory) to prevent memory pressure
5. WHERE an image file exceeds 4096 pixels on any dimension, THE viewer SHALL subsample it during decode to prevent out-of-memory errors
6. ALL document decoding (image bitmap, PDF pages) SHALL happen off the main thread to prevent ANR
7. WHILE a document is being decoded, THE viewer SHALL show a loading indicator
8. THE document viewer SHALL display a Share button in the top app bar that launches Android's system share sheet (ACTION_SEND)
9. THE Share action SHALL use FileProvider to provide the decrypted file via a content URI with correct MIME type
10. TEMPORARY shared files SHALL be cleaned up on Activity pause

### Requirement 39: GPS Tracking Settings Gate

**User Story:** As a user, I want background GPS tracking to be controlled from the Settings page under experimental features, so that I can opt in or out of location logging.

#### Acceptance Criteria

1. THE Settings screen SHALL include a "Background GPS Tracking" toggle within the Experimental Features section
2. THE GPS tracking toggle SHALL only be visible when the Experimental Features master toggle is ON
3. WHEN GPS tracking is OFF, THE Document Manager SHALL NOT start the LocationTrackingService
4. WHEN GPS tracking is ON, THE Document Manager SHALL start background location logging as configured
5. THE GPS tracking preference SHALL persist across app restarts (SharedPreferences)
6. THE default state for GPS tracking SHALL be OFF

### Requirement 40: Pull-to-Refresh on Document Screens

**User Story:** As a user, I want to pull down on the home page and document list to refresh content, so that I see the latest state of my documents.

#### Acceptance Criteria

1. THE home screen SHALL support a pull-to-refresh gesture that triggers document list reload
2. THE document list screen ("All Documents") SHALL support pull-to-refresh
3. WHILE refreshing, THE app SHALL display a LinearProgressIndicator at the top of the content area
4. THE refresh action SHALL complete within 2 seconds and dismiss the indicator
5. THE refresh SHALL re-query the document repository for current data


### Requirement 41: DICOM Medical Image Viewer (Experimental)

**User Story:** As a user with medical imaging files, I want to view DICOM images directly in the app without needing a separate medical viewer.

#### Acceptance Criteria

1. THE app SHALL parse DICOM files using a custom parser (no third-party DICOM library)
2. THE parser SHALL extract pixel data, image dimensions, bit depth, and photometric interpretation
3. THE viewer SHALL apply window/level contrast adjustment for grayscale images
4. THE viewer SHALL support 8-bit and 16-bit grayscale and RGB DICOM images
5. THIS feature SHALL be gated behind the "Extended Image Formats" experimental toggle

### Requirement 42: Per-Document PIN Protection

**User Story:** As a user, I want to lock individual documents with a PIN so that even if someone accesses my phone, specific sensitive documents remain protected.

#### Acceptance Criteria

1. THE user SHALL be able to set a PIN (4+ characters) on any individual document
2. WHEN a PIN is set, THE viewer SHALL NOT show the document preview until the correct PIN is entered
3. THE PIN SHALL be derived into an encryption key using PBKDF2-SHA256 (10K iterations)
4. IF the user forgets the PIN, THE document SHALL be permanently inaccessible (no recovery)
5. THE user SHALL be able to change the PIN (requires current PIN verification first)
6. THE user SHALL be able to remove the PIN (requires current PIN verification)
7. PIN-protected documents SHALL be tagged with system tag __PIN_PROTECTED and shown in a "Protected" folder

### Requirement 43: Import from URL

**User Story:** As a user, I want to import documents by pasting a web URL so that I can quickly add files from the internet.

#### Acceptance Criteria

1. THE import screen SHALL offer an "Import from URL" option
2. THE user SHALL be able to paste any HTTP/HTTPS URL pointing to a file
3. THE app SHALL download the file in the background with a progress indicator
4. THE app SHALL auto-detect the file format from URL extension or Content-Type header
5. GitHub blob URLs SHALL be automatically converted to raw download URLs
6. THE download SHALL be cancellable by the user

### Requirement 44: WiFi Document Sharing (Experimental)

**User Story:** As a user, I want to share my documents over WiFi so that other devices on my network can browse and download them through a web browser.

#### Acceptance Criteria

1. THE app SHALL start an embedded HTTP server on the local WiFi network
2. THE web interface SHALL display documents organized by tags
3. THE web interface SHALL support download, upload, rename, and tag management
4. ALL web requests SHALL require a randomly generated access token (shown in the app)
5. THE server SHALL stop when the user taps "Stop Sharing" or navigates away
6. THIS feature SHALL be gated behind its own experimental sub-toggle

### Requirement 45: Audio Playback and Android Auto (Experimental)

**User Story:** As a user, I want to import and play audio files (MP3) and have them accessible through Android Auto in my car.

#### Acceptance Criteria

1. THE app SHALL support importing MP3, M4A, WAV, OGG, and FLAC audio files
2. IMPORTED audio files SHALL be automatically tagged with system tag __AUDIO
3. THE app SHALL provide a MediaBrowserService for Android Auto integration
4. THE media tree SHALL organize audio by tags as browsable folders
5. PLAYBACK SHALL support play, pause, skip next, skip previous controls
6. THIS feature SHALL be gated behind the "Audio Playback & Android Auto" experimental toggle

### Requirement 46: End User License Agreement

**User Story:** As the app developer, I want users to accept a comprehensive EULA before using the app so that legal liability is clearly defined.

#### Acceptance Criteria

1. ON first launch, THE app SHALL display the EULA before any other screen
2. THE user MUST tap "I Accept" to proceed; "I Decline" SHALL close the app
3. THE acceptance SHALL be persisted with timestamp and GPS location
4. THE user SHALL be able to review the signed EULA from the gear menu ("License Agreement")
5. ON factory reset, THE EULA acceptance SHALL be cleared and shown again on next launch

### Requirement 47: Dark Theme Support

**User Story:** As a user, I want to switch between light and dark themes so that I can use the app comfortably in all lighting conditions.

#### Acceptance Criteria

1. THE Settings screen SHALL provide a "Dark Theme" toggle
2. TOGGLING the theme SHALL take effect immediately (activity recreate)
3. ALL non-experimental screens SHALL be readable in both light and dark themes
4. DOCUMENT preview areas SHALL maintain white background (documents need consistent viewing)

### Requirement 48: Review and Classify Documents

**User Story:** As a user, I want a dedicated screen to batch-tag untagged documents and review OCR results that need manual verification.

#### Acceptance Criteria

1. THE "Review & Classify" screen SHALL have two tabs: "Untagged" and "OCR Review"
2. THE Untagged tab SHALL allow multi-select and batch tag assignment
3. THE OCR Review tab SHALL show documents flagged for manual review with their extracted metadata
4. THE user SHALL be able to mark documents as "Reviewed" to dismiss them from the list

### Requirement 49: Share Target (Receive Documents from Other Apps)

**User Story:** As a user, I want to share documents from other apps (browser, file manager, email) directly into Document Manager.

#### Acceptance Criteria

1. THE app SHALL register as a share target for PDF, image, and video MIME types
2. WHEN a document is shared to the app, THE import pipeline SHALL run automatically
3. THE user SHALL see import progress and result (success/failure)
4. BIOMETRIC authentication SHALL be required before the import proceeds
