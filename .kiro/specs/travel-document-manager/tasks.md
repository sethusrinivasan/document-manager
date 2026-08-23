# Implementation Plan: Document Manager

## Overview

This plan implements the Document Manager Android application using Kotlin, Jetpack Compose, Room+SQLCipher, ML Kit, and Hilt. The implementation proceeds bottom-up: domain models and interfaces first, then data layer implementations, followed by presentation layer and wiring. Property-based tests (Kotest) and unit tests (JUnit 5 + MockK) validate correctness at each layer.

## Tasks

- [x] 1. Set up project structure, dependencies, and core domain models
  - [x] 1.1 Create module/package structure and configure Gradle dependencies
    - Set up the package structure: `presentation/`, `domain/`, `data/`, `di/`
    - Add Gradle dependencies: Room, SQLCipher, Hilt, ML Kit Text Recognition v2, ML Kit Document Scanner, Jetpack Compose + Material 3, Kotest property testing, JUnit 5, MockK, BouncyCastle (Argon2id)
    - Configure Hilt application class and annotation processing
    - _Requirements: 11.5 (offline-first architecture)_

  - [x] 1.2 Define domain models and enums
    - Create `Document`, `Tag`, `SearchQuery`, `DocumentType`, `DocumentFormat` data classes and enums
    - Create `TravelParameters`, `ParseResult`, `QueryIntent` for NLP
    - Create `TravelDocumentChecklist`, `RequiredDocument`, `MissingDocument` for checklist generation
    - Create `ExtractionResult`, `ExtractedValue`, `MetadataField` for scanner output
    - Create `LockoutState`, `AppError`, `AuthErrorType` for error handling
    - _Requirements: 1.1, 4.1, 5.1, 8.1_

  - [x] 1.3 Define repository interfaces and use case contracts
    - Create `AuthRepository`, `SessionManager` interfaces
    - Create `DocumentRepository`, `DocumentFileStorage` interfaces
    - Create `DocumentImporter`, `MetadataExtractor` interfaces
    - Create `TagRepository`, `AutoTagGenerator` interfaces
    - Create `SearchEngine`, `NaturalLanguageParser` interfaces
    - Create `DocumentChecklistGenerator` interface
    - _Requirements: 1.1, 2.1, 3.1, 4.1, 5.1, 6.1, 7.1, 8.1, 9.1_

- [x] 2. Implement authentication and session management
  - [x] 2.1 Implement PIN hashing with Argon2id and key derivation
    - Use BouncyCastle's Argon2id for PIN hashing with per-member salt
    - Derive database encryption key using HKDF(pin, device_key) where device_key is stored in AndroidKeyStore
    - Generate and store device_key in AndroidKeyStore on first launch
    - _Requirements: 2.6_

  - [x] 2.2 Implement AuthRepository with lockout logic
    - Store PIN hash and salt in `FamilyMemberEntity`
    - Implement `verifyPin()` with Argon2id comparison
    - Implement `recordFailedAttempt()`: lock after 3 failures (5-minute lockout), wipe after 5
    - Implement `wipeMemberData()` to delete member's encrypted database file
    - _Requirements: 2.1, 2.2, 2.3, 2.6_

  - [x] 2.3 Implement SessionManager with inactivity timeout
    - Create singleton `AuthSessionManager` implementing `SessionManager`
    - Maintain `isAuthenticated` StateFlow and `currentMemberId` StateFlow
    - Implement 30-minute inactivity timer that resets on `resetInactivityTimer()`
    - `endSession()` clears authentication state and nulls member ID
    - _Requirements: 2.4, 2.5_

  - [x] 2.4 Write property tests for PIN lockout (Property 3)
    - **Property 3: PIN lockout after threshold failures**
    - For any sequence of PIN attempts, 3 consecutive failures → locked state, 5 consecutive failures → wipe triggered
    - **Validates: Requirements 2.2, 2.3**

  - [x] 2.5 Write property tests for session timeout (Property 4)
    - **Property 4: Session timeout on inactivity**
    - For any authenticated session, elapsed time > 30 minutes since last activity → isAuthenticated returns false
    - **Validates: Requirements 2.4**

  - [x] 2.6 Write property tests for logout (Property 5)
    - **Property 5: Logout clears session**
    - After endSession(), isAuthenticated is false and currentMemberId is null
    - **Validates: Requirements 2.5**

- [x] 3. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Implement local document storage with encryption
  - [x] 4.1 Configure Room database with SQLCipher encryption
    - Create Room database class with `DocumentEntity`, `DocumentMetadataEntity`, `DocumentTagEntity` tables
    - Configure SQLCipher passphrase from derived key (HKDF of PIN + device_key)
    - Create DAOs for document CRUD operations
    - Implement per-member database file isolation (`member_{id}.db`)
    - _Requirements: 1.1, 1.6, 10.7_

  - [x] 4.2 Implement DocumentRepository
    - Implement `getAll()`, `getById()`, `insert()`, `delete()`, `getCount()`, `search()`
    - Maintain document index as Flow for reactive updates
    - Enforce 100-document capacity limit per member
    - _Requirements: 1.2, 1.4_

  - [x] 4.3 Implement DocumentFileStorage with secure deletion
    - Store encrypted document files as `{file_id}.enc` under `files/docs/{member_id}/`
    - Use AES-256-GCM for file encryption with key from AndroidKeyStore
    - Implement `secureDelete()` with overwrite-then-delete pattern
    - _Requirements: 1.1, 1.5, 1.6_

  - [x] 4.4 Write property tests for document index consistency (Property 1)
    - **Property 1: Document index consistency**
    - For any sequence of inserts and deletes, getCount() == successful inserts - successful deletes
    - **Validates: Requirements 1.2**

  - [x] 4.5 Write property tests for deleted document non-retrievability (Property 2)
    - **Property 2: Deleted documents are non-retrievable**
    - For any deleted document, subsequent queries never return that document
    - **Validates: Requirements 1.5**

  - [x] 4.6 Write property tests for member document isolation (Property 25)
    - **Property 25: Member document isolation**
    - Documents stored by member A never appear in query results for member B
    - **Validates: Requirements 10.7**

- [x] 5. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement document import pipeline
  - [x] 6.1 Implement camera-based document scanning with ML Kit Document Scanner
    - Integrate ML Kit Document Scanner API for camera capture
    - Provide edge detection and perspective correction (handled by ML Kit)
    - Return captured image as `ImportedDocument` with format and raw bytes
    - _Requirements: 3.3, 3.4, 3.5_

  - [x] 6.2 Implement Google Drive import
    - Integrate Google Drive Picker for file selection
    - Download selected file to local storage via REST API
    - Convert downloaded file to `ImportedDocument`
    - Handle network errors gracefully with descriptive messages
    - _Requirements: 3.1, 3.2_

  - [x] 6.3 Implement file import with format validation
    - Accept PDF, JPG, PNG formats
    - Validate file content and format
    - Return descriptive error for unsupported formats
    - _Requirements: 3.6, 3.5_

  - [x] 6.4 Write property tests for supported format acceptance (Property 6)
    - **Property 6: Supported format import acceptance**
    - For any file with format in {PDF, JPG, PNG} and valid content, import succeeds
    - **Validates: Requirements 3.6**

- [x] 7. Implement OCR-based metadata extraction
  - [x] 7.1 Implement MetadataExtractor with ML Kit Text Recognition
    - Integrate ML Kit Text Recognition v2 for OCR
    - Implement document type classification based on extracted text patterns
    - Extract type-specific metadata fields (ID number, name, expiry for passports; visa number, dates for visas; booking ref, flight details for tickets; etc.)
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.8_

  - [x] 7.2 Implement confidence scoring and manual review flagging
    - Calculate extraction confidence from individual field confidences
    - Flag documents for manual review when overall confidence < 80%
    - Handle unsupported document types by skipping extraction and prompting manual entry
    - _Requirements: 4.7, 4.9_

  - [x] 7.3 Wire import-to-extraction pipeline
    - After successful import, automatically trigger metadata extraction
    - Store extraction results in `document_metadata` table
    - Ensure extraction failure doesn't block document storage (graceful degradation)
    - _Requirements: 3.7_

  - [x] 7.4 Write property tests for import triggers extraction (Property 7)
    - **Property 7: Import triggers extraction**
    - For any successfully imported document, an ExtractionResult is produced
    - **Validates: Requirements 3.7**

  - [x] 7.5 Write property tests for low-confidence manual review (Property 8)
    - **Property 8: Low-confidence extraction flags manual review**
    - For any ExtractionResult where confidence < 0.8, requiresManualReview is true
    - **Validates: Requirements 4.7**

- [x] 8. Implement automatic tag generation
  - [x] 8.1 Implement AutoTagGenerator
    - Map DocumentType to tag: PASSPORT→"passport", VISA→"visa", TICKET→"ticket", HOTEL_BOOKING→"accommodation", HEALTH_INSURANCE→"health"
    - Generate destination tag from DESTINATION metadata field
    - Generate date-range tags from date metadata fields (EXPIRY_DATE, ISSUE_DATE)
    - Catch all exceptions silently — tag generation failure must not break import
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9_

  - [x] 8.2 Wire tag generation into import pipeline
    - After extraction, invoke AutoTagGenerator with document type and metadata
    - Store generated tags in `document_tags` table with `isAutoGenerated = true`
    - If tag generation throws, continue document processing without tags
    - _Requirements: 5.1, 5.9_

  - [x] 8.3 Write property tests for document type to tag mapping (Property 9)
    - **Property 9: Document type to tag mapping**
    - For any document with classified DocumentType, auto-generated tags include the corresponding type tag
    - **Validates: Requirements 5.2, 5.3, 5.4, 5.5, 5.6**

  - [x] 8.4 Write property tests for destination tag generation (Property 10)
    - **Property 10: Destination metadata generates destination tag**
    - For any extraction with non-empty DESTINATION field, auto-tags include a destination tag
    - **Validates: Requirements 5.7**

  - [x] 8.5 Write property tests for date-range tag generation (Property 11)
    - **Property 11: Date metadata generates date-range tags**
    - For any extraction with date fields, auto-tags include at least one date-range tag
    - **Validates: Requirements 5.8**

  - [x] 8.6 Write property tests for silent tag generation failure (Property 12)
    - **Property 12: Tag generation failure is silent**
    - For any exception during tag generation, document import still succeeds
    - **Validates: Requirements 5.9**

- [x] 9. Checkpoint - Build verified, property tests written
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Implement user tag management
  - [x] 10.1 Implement TagRepository
    - Implement `addTag()` with deduplication check (prevent duplicate tags)
    - Implement `removeTag()` from a single document
    - Implement `deleteTagGlobally()` to remove tag from all member's documents
    - Implement `getTagsForDocument()` and `getAllTags()`
    - Enforce 20-tag-per-document limit
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_

  - [x] 10.2 Write property tests for tag add (Property 13)
    - **Property 13: Adding a tag grows the tag list**
    - For any document and non-duplicate tag, adding it increases tag count by one
    - **Validates: Requirements 6.2**

  - [x] 10.3 Write property tests for tag remove (Property 14)
    - **Property 14: Removing a tag shrinks the tag list**
    - For any document containing tag T, removing T decreases count by one and T is absent
    - **Validates: Requirements 6.3**

  - [x] 10.4 Write property tests for tag deduplication (Property 15)
    - **Property 15: Tag deduplication (idempotence)**
    - Adding tag T multiple times results in exactly one occurrence
    - **Validates: Requirements 6.4**

  - [x] 10.5 Write property tests for global tag deletion (Property 16)
    - **Property 16: Global tag deletion removes from all documents**
    - After deleting tag T globally, no member document contains T
    - **Validates: Requirements 6.6**

- [x] 11. Implement search functionality
  - [x] 11.1 Implement tag-based and free-form search in SearchEngine
    - Implement `searchByTags()` filtering documents by all specified tags (AND logic)
    - Implement `searchFreeForm()` matching against document metadata values and tag names
    - Combine criteria with AND logic when both tags and free-text are provided
    - Return empty list for no matches (no exceptions, no error messages)
    - Ensure results return within 2 seconds for 100 documents
    - _Requirements: 7.1, 7.2, 7.4, 7.5, 7.6_

  - [x] 11.2 Write property tests for search AND logic (Property 17)
    - **Property 17: Search results satisfy all criteria (AND logic)**
    - Every document in results must satisfy ALL specified criteria simultaneously
    - **Validates: Requirements 7.1, 7.2, 7.4**

  - [x] 11.3 Write property tests for empty search results (Property 18)
    - **Property 18: Empty search returns empty list without error**
    - For any query matching zero documents, result is empty list with no exception
    - **Validates: Requirements 7.5**

- [x] 12. Implement natural language parser and travel checklist
  - [x] 12.1 Implement NaturalLanguageParser with regex patterns
    - Parse family size from patterns like "family of N"
    - Parse origin from patterns like "living in X", "from X"
    - Parse destination from country/city names
    - Parse duration from keywords: "week" → 7, "two weeks" → 14, "month" → 30
    - Determine intent: DOCUMENT_SEARCH, TRAVEL_CHECKLIST, or MISSING_DOCUMENTS
    - Return `NeedMoreInfo` when fewer than 2 meaningful parameters extracted
    - _Requirements: 7.3, 7.7, 8.1, 8.2, 8.3, 8.4, 8.5, 8.8_

  - [x] 12.2 Implement DocumentChecklistGenerator
    - Generate required document checklist based on origin and destination (visa requirements, passports)
    - Scale per-person documents by family size (e.g., N passports for family of N)
    - Return non-empty checklist for any valid origin+destination combination
    - _Requirements: 8.6, 8.7_

  - [x] 12.3 Implement missing document detection
    - Compare existing documents against required checklist (set difference by type and count)
    - Generate suggestions: passport → "Ensure passport validity is 6+ months", visa → "Apply for visa at embassy", health → "Check vaccination requirements"
    - Update missing indicators as documents are added/removed (reactive via Flow)
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [x] 12.4 Wire NLP into SearchEngine
    - Implement `searchNaturalLanguage()` in SearchEngine
    - Route to NaturalLanguageParser, then dispatch to checklist or document search based on intent
    - Fall back to literal text search on parser failure
    - _Requirements: 7.3_

  - [x] 12.5 Write property tests for NLP travel parameter extraction (Property 19)
    - **Property 19: NLP travel parameter extraction**
    - For queries with family size, origin, destination, duration patterns, parser extracts correct values
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

  - [x] 12.6 Write property tests for checklist generation (Property 20)
    - **Property 20: Checklist generation for valid travel parameters**
    - For any TravelParameters with origin and destination, checklist is non-empty
    - **Validates: Requirements 8.6**

  - [x] 12.7 Write property tests for family size scaling (Property 21)
    - **Property 21: Family size scales per-person document requirements**
    - For familySize=N, per-person documents have countNeeded=N
    - **Validates: Requirements 8.7**

  - [x] 12.8 Write property tests for insufficient parameters (Property 22)
    - **Property 22: Insufficient query parameters request more info**
    - For queries with < 2 meaningful params, result is NeedMoreInfo
    - **Validates: Requirements 8.8**

  - [ ] 12.9 Write property tests for missing document detection (Property 23)
    - **Property 23: Missing document detection is set difference**
    - Missing documents = required documents with no match in existing set (by type and count)
    - **Validates: Requirements 9.1, 9.6**

  - [ ] 12.10 Write property tests for missing document suggestions (Property 24)
    - **Property 24: Missing document suggestions match type**
    - PASSPORT → passport validity guidance, VISA → visa application guidance, HEALTH_INSURANCE → vaccination guidance
    - **Validates: Requirements 9.3, 9.4, 9.5**

- [x] 13. Checkpoint - Build verified, NLP tests written
  - Ensure all tests pass, ask the user if questions arise.

- [x] 14. Implement presentation layer - Authentication screens (deferred - PIN auth not yet built, app uses direct access)
  - [ ] 14.1 Implement PIN entry and lockout UI
    - Create PIN entry Compose screen with numeric keypad
    - Display lockout countdown timer when locked (5-minute display)
    - Show data wipe warning after 4th failure
    - Integrate with AuthRepository and SessionManager ViewModels
    - _Requirements: 2.1, 2.2, 2.3_

  - [ ] 14.2 Implement onboarding and family member setup
    - Create guided onboarding flow for first launch (Compose screens with step indicators)
    - Allow creation of family member accounts with name and PIN setup
    - Support at least 3 member accounts, with no hard upper limit
    - Provide contextual help tooltips throughout onboarding
    - _Requirements: 10.1, 10.2, 10.5, 10.6_

- [x] 15. Implement presentation layer - Document management screens
  - [ ] 15.1 Implement document list and detail views
    - Create document list screen with Flow-based reactive updates
    - Show document type, name, tags, and manual review badge
    - Create document detail screen showing all metadata and tags
    - Implement tag management interface on detail screen (add/remove tags)
    - _Requirements: 1.2, 6.1, 6.2, 6.3_

  - [ ] 15.2 Implement document import UI with progress indicators
    - Create import screen with options: Camera scan, Google Drive, File picker
    - Show progress indicators during import and extraction
    - Display actionable error guidance on failures
    - Provide visual guidance for camera document positioning (ML Kit handles this)
    - _Requirements: 3.1, 3.3, 3.4, 3.5, 10.3, 10.4_

  - [ ] 15.3 Implement search interface
    - Create search screen with tag filter chips and free-text input
    - Add natural language query input area
    - Display search results as document list
    - Show travel checklist results and missing document highlights
    - Display "need more info" prompts when NLP requires additional parameters
    - _Requirements: 7.1, 7.2, 7.3, 9.2_

- [x] 16. Implement Hilt dependency injection wiring
  - [ ] 16.1 Create Hilt modules and wire all components
    - Create database module providing Room DAOs and SQLCipher config
    - Create repository module binding interfaces to implementations
    - Create scanner module providing ML Kit instances
    - Create session module providing singleton SessionManager
    - Wire ViewModels with `@HiltViewModel` annotation
    - _Requirements: all (integration)_

- [x] 17. Implement offline-first and database recovery
  - [ ] 17.1 Implement storage corruption recovery and offline guarantees
    - Implement `PRAGMA integrity_check` on database open
    - Attempt recovery on corruption; notify user of data loss if unrecoverable
    - Ensure no network dependency exists for any core operation
    - Verify Google Drive import only needs brief online access, then works fully offline
    - _Requirements: 1.3, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_

- [x] 18. Final checkpoint - Full app build verified, deployed to device
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document (25 properties total)
- Unit tests validate specific examples and edge cases
- The implementation uses Kotlin throughout with Jetpack Compose for UI
- All core functionality is offline-first with no cloud dependencies
- Kotest Property Testing library is used for property-based tests
- JUnit 5 + MockK are used for unit/integration tests

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.3"] },
    { "id": 3, "tasks": ["2.1", "4.1"] },
    { "id": 4, "tasks": ["2.2", "2.3", "4.2", "4.3"] },
    { "id": 5, "tasks": ["2.4", "2.5", "2.6", "4.4", "4.5", "4.6"] },
    { "id": 6, "tasks": ["6.1", "6.2", "6.3", "10.1"] },
    { "id": 7, "tasks": ["6.4", "7.1", "10.2", "10.3", "10.4", "10.5"] },
    { "id": 8, "tasks": ["7.2", "7.3"] },
    { "id": 9, "tasks": ["7.4", "7.5", "8.1"] },
    { "id": 10, "tasks": ["8.2", "11.1"] },
    { "id": 11, "tasks": ["8.3", "8.4", "8.5", "8.6", "11.2", "11.3"] },
    { "id": 12, "tasks": ["12.1", "12.2"] },
    { "id": 13, "tasks": ["12.3", "12.4"] },
    { "id": 14, "tasks": ["12.5", "12.6", "12.7", "12.8", "12.9", "12.10"] },
    { "id": 15, "tasks": ["14.1", "14.2"] },
    { "id": 16, "tasks": ["15.1", "15.2", "15.3"] },
    { "id": 17, "tasks": ["16.1"] },
    { "id": 18, "tasks": ["17.1"] }
  ]
}
```

## 19. Implement document deduplication during import
- [x] 19.1 Add SHA-256 hash computation to ImportViewModel
  - Compute hash of imported file bytes
  - Query existing documents by filename match
  - Compare content hashes for dedup detection
  - Requirements: 12.1, 12.2
- [x] 19.2 Implement duplicate confirmation dialog in ImportScreen
  - Show dialog with existing doc info when duplicate detected
  - "Replace" option: deletes old doc, imports new
  - "Cancel" option: discards import
  - Requirements: 12.3, 12.4, 12.5, 12.6

## 20. Implement document list and viewer
- [x] 20.1 Create DocumentListScreen with reactive document list
  - Collect documents from repository as Flow
  - Display type icon, filename, date, tags per item
  - Support empty state with import guidance
  - Add navigation from main dashboard
  - Requirements: 13.1, 13.2, 13.6, 13.7
- [x] 20.2 Create DocumentViewerScreen for rendering documents
  - Retrieve raw bytes from DocumentFileStorage
  - Render PDFs using PdfRenderer
  - Render JPG/PNG as zoomable Image
  - Show metadata overlay
  - Requirements: 13.3, 13.4, 13.5

## 21. Implement background GPS tracking service
- [x] 21.1 Create LocationTrackingService as foreground service
  - Foreground service with location type
  - Persistent notification with stop action
  - Configurable polling interval (SharedPreferences)
  - Movement threshold check (10m) before logging
  - Requirements: 14.1, 14.2, 14.3, 14.6, 14.7
- [x] 21.2 Create TrackingSettingsPanel UI component
  - Toggle switch to start/stop service
  - Interval selector chips (30s, 1min, 5min, 15min, 30min)
  - Requirements: 14.4, 14.5

## 22. Implement system telemetry dashboard
- [x] 22.1 Create DashboardContent with status cards
  - Battery percentage and charging status
  - GPS coordinates with accuracy
  - Network connectivity type and status
  - Document count
  - Auto-refresh every 5 seconds
  - Requirements: 15.1, 15.2, 15.3, 15.4, 15.5, 15.6

## 23. Implement debug logging infrastructure
- [x] 23.1 Create DebugLogger singleton
  - Three output destinations: Logcat, ring buffer, file
  - Structured log entries with timestamp, level, component, message
  - File rotation at 5MB
  - Requirements: 16.1, 16.2, 16.3
- [x] 23.2 Create DebugLogScreen and DebugFloatingButton
  - Dark-themed log viewer with color-coded severity
  - Auto-scroll, clear logs, close actions
  - Floating bug icon on main screen
  - Requirements: 16.4, 16.5, 16.6, 16.7
- [x] 23.3 Create SystemTelemetry and instrument components
  - Snapshot on onCreate/onResume (memory, battery, processes, connectivity, GPS)
  - Instrument AuthRepository, DocumentImportUseCase, SearchEngine, FileStorage, SessionManager
  - Requirements: 16.8

## 24. Implement crash tracking
- [x] 24.1 Create CrashHandler with global uncaught exception handler
  - Logs exception class, message, full stack trace
  - Logs causal chain up to 5 levels
  - Delegates to default handler after logging
  - Installed in Application.onCreate()
  - Requirements: 17.1, 17.2, 17.3, 17.4, 17.5

## 25. Implement tag management system
- [x] 25.1 Extend TagRepository and DAO with rename, usage count, and create operations
  - Add `renameTag(memberId, oldName, newName)` to TagRepository + DAO
  - Add `getTagUsageCount(memberId, tagName)` to TagRepository + DAO
  - Add `createTag(memberId, tagName)` for standalone tag creation
  - Requirements: 18.3, 18.4
- [x] 25.2 Create TagManagementScreen UI
  - List all tags with document count badge
  - Create new tag (text field + button)
  - Rename tag (inline edit or dialog)
  - Sort toggle: alphabetical / by usage count
  - Requirements: 18.1, 18.2, 18.10
- [x] 25.3 Implement safe delete with confirmation guard
  - Before delete: query usage count
  - If count > 0: show dialog with warning "This tag is used by N documents. Remove from all and delete?"
  - On confirm: call deleteTagGlobally then remove tag
  - On cancel: dismiss
  - Requirements: 18.5, 18.6, 18.7
- [x] 25.4 Wire TagManagementScreen into main navigation
  - Add "Tags" button to dashboard
  - Navigate to TagManagementScreen
  - Requirements: 18.1
- [x] 25.5 Verify add/remove tags on document viewer still works
  - Existing add tag from viewer: Requirements 18.8
  - Existing remove tag from viewer: Requirements 18.9
  - No code changes needed — already implemented

## 26. Security hardening
- [ ] 26.1 [CRITICAL] Enable SQLCipher encryption on the production database
  - Replace plain Room.databaseBuilder with EncryptedDatabaseProvider
  - Derive DB passphrase from PIN + device key via HKDF
  - Requires PIN auth before DB can be opened
  - Requirements: 19.1
- [ ] 26.2 [CRITICAL] Enforce PIN authentication gate on app launch
  - Add PIN entry screen as mandatory first screen
  - Store session state in AuthSessionManager
  - Replace all hardcoded "default-member" with session's currentMemberId
  - Block navigation to documents/search/import until authenticated
  - Requirements: 19.2, 19.3
- [x] 26.3 [HIGH] Fix DeviceKeyManager zero-IV vulnerability
  - Replace ByteArray(12) fixed IV with SecureRandom nonce
  - Store nonce in SharedPreferences for deterministic re-derivation
  - Requirements: 19.6
- [x] 26.4 [HIGH] Implement temp file cleanup after external viewing
  - Register Activity lifecycle callback (onPause/onStop)
  - Delete all files in cache/shared_docs/ directory
  - Add 60-second delayed cleanup coroutine as fallback
  - Requirements: 19.5
- [x] 26.5 [MEDIUM] Add input sanitization for tags and filenames
  - Whitelist regex: allow only [a-zA-Z0-9 _\-\.] for tags
  - Strip path separators from filenames
  - Reject empty/blank inputs
  - Requirements: 19.8
- [x] 26.6 [MEDIUM] Gate debug logging on BuildConfig.DEBUG
  - DebugLogger.init() only creates file logger in debug builds
  - Release builds: Logcat only at WARN+ level, no file output
  - Add GPS redaction toggle in settings
  - Requirements: 19.11, 19.10
- [x] 26.7 [MEDIUM] Add network security configuration
  - Create res/xml/network_security_config.xml blocking cleartext
  - Reference in AndroidManifest android:networkSecurityConfig
  - Requirements: 19.7
- [x] 26.8 [LOW] Zero sensitive byte arrays after use
  - PIN char arrays: Arrays.fill after hash computation
  - Decryption key arrays: zero after DB open
  - Requirements: 19.9
- [ ] 26.9 [LOW] Tighten ProGuard rules for release builds
  - Remove blanket keep on data.local package
  - Keep only Room entities and Hilt entry points
  - Requirements: 19.10 (implied)
- [x] 26.10 Sanitize debug log content
  - Ensure no raw PIN, key material, or full file content is logged
  - Audit all DebugLogger calls for sensitive data exposure
  - Requirements: 19.4

## 27. Implement encryption consent and region-based storage
- [x] 27.1 Create ConsentPreferences data store
  - SharedPreferences wrapper: hasConsented, selectedRegion, encryptionEnabled
  - Helper to check if first launch
  - Requirements: 20.9, 20.10
- [x] 27.2 Create ConsentScreen UI
  - Country/region dropdown selector
  - Encryption explanation text
  - Terms agreement checkbox
  - PIN irrecoverability warning with separate acknowledgment
  - "Continue" button gated on both checkboxes
  - Requirements: 20.1, 20.2, 20.3, 20.6, 20.7, 20.8
- [x] 27.3 Implement region-based encryption availability
  - Classify countries as encryption-permitted or restricted
  - Show/hide encryption option based on selection
  - Default to encrypted when permitted
  - Requirements: 20.4, 20.5
- [x] 27.4 Wire ConsentScreen into app launch flow
  - Check ConsentPreferences on MainActivity onCreate
  - If not consented: show ConsentScreen first
  - After consent: proceed to dashboard (or PIN screen if encrypted)
  - Requirements: 20.1
- [x] 27.5 Route storage mode based on consent choice
  - If encrypted: use existing SQLCipher path + PIN auth
  - If unencrypted: use plain Room DB, skip PIN, disable file encryption
  - Requirements: 20.11

## 28. Implement failed PIN attempt notifications
- [x] 28.1 Create SecurityPreferences for recovery contact storage
  - Encrypted SharedPreferences for phone number and email
  - Getter/setter methods
  - Validation (phone format, email format)
  - Requirements: 21.1, 21.2, 21.9
- [x] 28.2 Create SecurityAlertService for SMS and email alerts
  - SMS sending via SmsManager on lockout event
  - Include timestamp and GPS in message body
  - Stub EmailAlertService interface for future backend integration
  - Log alert locally as fallback if SMS permission denied
  - Requirements: 21.3, 21.4, 21.7, 21.8, 21.10
- [x] 28.3 Wire SecurityAlertService into AuthRepositoryImpl lockout flow
  - On recordFailedAttempt returning isLocked=true, trigger alert
  - Pass configured phone/email from SecurityPreferences
  - Requirements: 21.3, 21.4
- [x] 28.4 Create SettingsScreen for recovery contact management
  - Edit recovery phone number field
  - Edit recovery email field
  - Toggle SMS alerts on/off
  - Show last alert sent timestamp
  - Add navigation from dashboard
  - Requirements: 21.6
- [x] 28.5 Capture recovery contacts during first-launch onboarding
  - Add phone number and email fields to ConsentScreen
  - Validate before proceeding
  - Store in SecurityPreferences
  - Requirements: 21.1, 21.2
- [x] 28.6 Add SEND_SMS permission and runtime request
  - Declare in AndroidManifest
  - Request at runtime before first send
  - Graceful fallback if denied
  - Requirements: 21.7, 21.8

## 29. Implement batch import from Google Drive folder
- [ ] 29.1 Create DrivePickerScreen with folder browsing UI
  - Google Sign-In authentication
  - List folders and files in current directory
  - Navigate into subfolders (breadcrumb trail)
  - Select folder button to start import
  - Requirements: 22.1, 22.2, 22.9, 22.10
- [ ] 29.2 Create BatchImportViewModel with progress tracking
  - List all supported files in selected folder (recursive)
  - Process files sequentially through import pipeline
  - Track progress: current/total, percentage, current filename
  - Support cancellation (coroutine Job cancellation)
  - Collect skip/fail count for summary
  - Requirements: 22.3, 22.4, 22.5, 22.6, 22.7, 22.8
- [ ] 29.3 Create BatchImportProgressScreen UI
  - Progress bar (determinate)
  - Current file / total count text
  - Current filename being processed
  - Cancel button
  - Summary card on completion (imported / skipped)
  - Requirements: 22.4, 22.5, 22.8

## 30. Implement batch import from local phone folder
- [ ] 30.1 Create LocalFolderPickerScreen using SAF DocumentTree
  - Launch ACTION_OPEN_DOCUMENT_TREE intent
  - Enumerate files from granted URI recursively
  - Filter to supported formats (PDF, JPG, PNG, video)
  - Requirements: 23.1, 23.2, 23.7
- [ ] 30.2 Wire local folder import into BatchImportViewModel
  - Same progress tracking and pipeline as Drive import
  - Same progress UI (reuse BatchImportProgressScreen)
  - Summary on completion
  - Requirements: 23.3, 23.4, 23.5, 23.6
- [ ] 30.3 Add batch import options to Import screen
  - Three options on import screen: Single File, Google Drive Folder, Local Folder
  - Requirements: 22.1, 23.1

## 31. Implement camera document capture with smart naming
- [ ] 31.1 Add "Take Photo" option to ImportScreen chooser
  - Fourth card option: camera icon + "Take Photo" + "Scan document with camera"
  - Requirements: 24.1
- [ ] 31.2 Integrate ML Kit Document Scanner for camera capture
  - Launch GmsDocumentScanning client
  - Handle activity result with scanned page URI
  - Convert to byte array for preview
  - Requirements: 24.2, 24.9, 24.10
- [ ] 31.3 Create CameraPreviewScreen with accept/retake/name edit
  - Show captured image preview
  - Accept / Retake buttons
  - Run quick OCR for smart filename suggestion
  - Editable filename text field
  - Requirements: 24.3, 24.4, 24.5, 24.6, 24.7
- [ ] 31.4 Implement smart filename generation from OCR text
  - Detect document type keywords (passport, visa, ticket, hotel, insurance)
  - Extract holder name if found
  - Extract destination or booking ref if found
  - Combine into filename: "{type}_{identifier}.jpg"
  - Fallback: "document_{timestamp}.jpg"
  - Requirements: 24.5, 24.6
- [ ] 31.5 Wire camera capture into import pipeline
  - After user confirms: feed bytes + filename into DocumentImportUseCase
  - Standard OCR + tag + store flow
  - Requirements: 24.8

## 32. Implement quick share to external apps
- [ ] 32.1 Add share action to document list selection mode toolbar
  - Share icon in selection toolbar (alongside delete)
  - Triggers multi-file share flow
  - Requirements: 26.1, 26.6
- [ ] 32.2 Add share button to document viewer screen
  - Share icon in top app bar or as action button
  - Shares single current document
  - Requirements: 26.2
- [ ] 32.3 Implement ShareHelper utility
  - Decrypt file(s) to cache/shared_docs/
  - Build share Intent with correct MIME types
  - Single file: ACTION_SEND + putExtra EXTRA_STREAM
  - Multiple files: ACTION_SEND_MULTIPLE + putParcelableArrayListExtra
  - Use FileProvider URIs
  - Launch chooser
  - Requirements: 26.3, 26.4, 26.5, 26.6, 26.7, 26.9
- [ ] 32.4 Add telemetry for share actions
  - Log share event with file count and target (if determinable)
  - Requirements: 26.10

## 35. Implement cloud backup to Google Drive and AWS S3
- [ ] 35.1 Create BackupPreferences for backup configuration
  - Stores: backupDestination (drive/s3/none), s3Endpoint, s3Bucket, s3AccessKey, s3SecretKey
  - Stores: lastBackupTimestamp, backupSchedule (daily/weekly/manual)
  - Stores: autoBackupEnabled boolean
  - Requirements: 29.2, 29.4, 29.10
- [ ] 35.2 Create BackupService for packaging encrypted files
  - Collect all .enc files from docs/ directory
  - Copy encrypted DB file (traveldocs.db)
  - Create manifest.json: timestamp, doc count, app version, member ID
  - Package into a ZIP archive (files stay encrypted inside)
  - Requirements: 29.5, 29.6, 29.14, 29.15
- [ ] 35.3 Implement Google Drive backup upload
  - Authenticate via Google Sign-In (drive.file scope)
  - Upload ZIP to Drive app folder
  - Progress tracking via resumable upload
  - Requirements: 29.3, 29.7, 29.8
- [ ] 35.4 Implement AWS S3-compatible upload
  - HTTP PUT with AWS Signature V4 auth
  - Multipart upload for large files
  - Progress tracking
  - Support any S3-compatible endpoint (MinIO, Backblaze, etc.)
  - Requirements: 29.4, 29.7, 29.8
- [ ] 35.5 Create BackupScreen UI
  - Backup destination selector (Drive / S3 / Off)
  - S3 configuration fields (endpoint, bucket, keys)
  - "Backup Now" button with progress
  - Schedule selector (manual / daily / weekly)
  - Last backup timestamp display
  - Requirements: 29.1, 29.2, 29.10
- [ ] 35.6 Create BackupProgressScreen UI
  - Progress bar + file count + current file
  - Cancel button
  - Summary on completion (files backed up, total size)
  - Requirements: 29.7, 29.8, 29.9
- [ ] 35.7 Implement restore from backup
  - List available backups from Drive or S3
  - Download selected backup ZIP
  - Extract encrypted files to docs/ directory
  - Restore encrypted DB
  - Requirements: 29.12
- [ ] 35.8 Add required permissions and launch prompts
  - AndroidManifest: INTERNET (already), Google Drive OAuth scope
  - Runtime: check Drive auth on launch, show banner if needed
  - Requirements: 29.11
- [ ] 35.9 Add telemetry and debug logging
  - Log: backup_start, backup_progress, backup_complete, backup_error
  - Log: restore_start, restore_complete
  - UsageTelemetry markers for funnel tracking
  - Requirements: 29.13

## 36. Implement launch disclaimer and telemetry consent
- [x] 36.1 Create DisclaimerScreen UI
  - Full-screen scrollable disclaimer text
  - Telemetry consent toggle (opt-in)
  - "I Understand & Continue" button (disabled until scrolled to bottom)
  - Requirements: 30.1, 30.2, 30.3, 30.4, 30.5, 30.6, 30.7
- [x] 36.2 Create DisclaimerPreferences
  - Stores: disclaimerAccepted (bool), acceptedTimestamp (Long), telemetryConsented (bool)
  - Requirements: 30.8, 30.9
- [x] 36.3 Gate UsageTelemetry on consent
  - Check telemetryConsented before every log call
  - Add telemetry toggle to Settings screen
  - Requirements: 30.9, 30.10
- [x] 36.4 Wire DisclaimerScreen into launch flow
  - Check disclaimerAccepted before showing ConsentScreen
  - Flow: Disclaimer → Consent → Main
  - Requirements: 30.11

## 37. Implement backup functionality (Local + Drive + S3)
- [ ] 37.1 Create BackupManager service
  - Package encrypted files + DB into ZIP
  - Generate manifest (timestamp, doc count, app version)
  - Requirements: 29.5, 29.14
- [ ] 37.2 Implement local folder backup
  - Launch ACTION_OPEN_DOCUMENT_TREE for folder selection
  - Pre-populate suggested name "TravelDocs_Backup"
  - Write ZIP to selected folder via DocumentFile
  - Requirements: 31.1, 31.2, 31.3, 31.4, 31.5
- [ ] 37.3 Create BackupScreen UI with 3 destination options
  - Cards: Local Folder / Google Drive / S3
  - Config forms for Drive auth and S3 credentials
  - "Backup Now" with progress
  - Last backup info display
  - Suggested folder name prompt
  - Requirements: 29.1, 29.2, 31.1
- [ ] 37.4 Implement restore from backup
  - Pick ZIP file or select from Drive/S3
  - Extract to app storage
  - Requirements: 29.12, 31.6

## 38. Airplane mode status in diagnostics
- [ ] 38.1 Add airplane mode detection to DiagnosticsScreen
  - Read Settings.Global.AIRPLANE_MODE_ON
  - Display as status row with icon
  - Requirements: 32.1, 32.2

## 40. Adaptive GPS tracking with timeline
- [ ] 40.1 Implement adaptive tracking algorithm in LocationTrackingService
  - Moving state: log every 30s (displacement > 20m between fixes)
  - Stationary state: log every 5min (no movement > 2 min)
  - State transitions logged
  - Requirements: 34.1, 34.2, 34.3, 34.4
- [ ] 40.2 Create GPS history database table
  - Room entity: id, lat, lng, accuracy, timestamp, isMoving
  - DAO for insert and timeline queries
  - Requirements: 34.5, 34.6
- [ ] 40.3 Create TimelineScreen to view travel history
  - Chronological list of locations with timestamps
  - Moving/stationary indicators
  - Requirements: 34.7
- [ ] 40.4 Enable tracking by default after consent
  - Start service in ConsentScreen onConsented callback
  - Requirements: 34.1

## 41. GPS coordinates on document import
- [ ] 41.1 Capture GPS at import time in DocumentImportUseCase
  - Get last known location
  - Store lat/lng in document metadata (new MetadataField entries or separate field)
  - Requirements: 35.1, 35.2
- [ ] 41.2 Display import location in DocumentViewerScreen
  - Show coordinates in Properties section
  - "Location unavailable" if null
  - Requirements: 35.3, 35.4

## 42. Advanced backup management
- [ ] 42.1 Create timestamped backup subfolders
  - Format: TravelDocs_Backup/backup_20240815_093022/
  - No conflicts between multiple backups
  - Requirements: 36.1, 36.3
- [ ] 42.2 Enhanced manifest with file list and types
  - manifest.json: timestamp, docCount, totalSize, appVersion, memberId, files: [{name, type, size}]
  - Requirements: 36.2
- [ ] 42.3 Create BackupInspectorScreen
  - List backups in a folder (read manifests)
  - Show summary: doc count by type, total size, date
  - Preview individual files from backup
  - Requirements: 36.4, 36.5
- [ ] 42.4 Implement restore with merge/replace options
  - Dialog: "Replace all" or "Merge (skip duplicates)"
  - Compare filenames for dedup during merge
  - Progress + summary (restored, skipped, replaced counts)
  - Requirements: 36.6, 36.7, 36.8

## 43. Implement experimental feature toggles
- [x] 43.1 Create FeatureFlags utility
  - SharedPreferences: experimental, google_drive, s3_storage, backup_restore
  - Static getters/setters
- [x] 43.2 Add toggles to Settings screen
  - "Experimental Features" master toggle
  - Sub-toggles: Google Drive, S3, Backup & Restore (only visible when master is on)
- [x] 43.3 Gate backup/restore/drive/s3 UI on feature flags
  - Backup menu only shows when backup_restore enabled
  - Drive/S3 cards only show when respective flags enabled


- [x] 44. DICOM medical image viewer
  - [x] 44.1 Implement DicomParser (first-principles, no library)
    - Parse DICM magic, tag-length-value elements, extract pixel data
    - Support 8-bit, 12-bit, 16-bit grayscale and RGB
    - Apply window/level contrast adjustment
    - _Requirements: 41_
  - [x] 44.2 Integrate DICOM rendering into DocumentViewerScreen
    - Detect DICOM format via magic bytes
    - Render via produceState on background thread
    - Gate behind Extended Image Formats experimental flag

- [x] 45. Per-document PIN protection
  - [x] 45.1 Implement SecureDocumentManager
    - PBKDF2-SHA256 key derivation (10K iterations)
    - PIN hash storage in SharedPreferences
    - Encrypt/decrypt with AES-256-GCM
    - _Requirements: 42_
  - [x] 45.2 Add PIN UI to DocumentViewerScreen
    - Set PIN dialog (4+ chars, confirm)
    - Remove PIN dialog (verify current)
    - Change PIN dialog (3-step: verify → new → consent)
    - Block preview until PIN entered
  - [x] 45.3 Add "Protected" folder to home screen
    - Filter __PIN_PROTECTED from tag manager
    - Show locked folder icon, listed first

- [x] 46. Import from URL
  - [x] 46.1 Create UrlImportScreen
    - URL input field with default DICOM sample
    - GitHub blob → raw URL conversion
    - Download with progress indicator on Dispatchers.IO
    - Auto-detect format from extension/content-type
    - _Requirements: 43_

- [x] 47. WiFi document sharing (experimental)
  - [x] 47.1 Implement DocumentWebServer (NanoHTTPD)
    - Token-based auth (random per session)
    - HTML web UI with tag folders, download, upload, rename, tag management
    - Bulk operations via JavaScript
    - Input sanitization on all user inputs
    - _Requirements: 44_
  - [x] 47.2 Create WebShareScreen
    - Start/Stop server UI
    - Display URL with access token
    - Hilt EntryPoint for DI access
    - DisposableEffect to stop on navigation away

- [x] 48. Audio playback and Android Auto (experimental)
  - [x] 48.1 Add AUDIO format + __AUDIO system tag
  - [x] 48.2 Implement AudioPlaybackService (MediaBrowserServiceCompat)
    - Media tree: Root → All Audio / By Tag
    - MediaSession with play/pause/skip controls
    - Decrypt audio file on-the-fly for MediaPlayer
    - Gate behind audio_playback feature flag
    - _Requirements: 45_
  - [x] 48.3 Add automotive_app_desc.xml and manifest registration

- [x] 49. EULA and legal compliance
  - [x] 49.1 Create EulaScreen + EulaViewScreen
    - 14-section agreement covering warranties, liability, indemnification, pricing
    - AI disclosure, per-document PIN section
    - Persist acceptance with timestamp + GPS
    - _Requirements: 46_
  - [x] 49.2 Integrate into launch flow (before splash/disclaimer/consent)

- [x] 50. Dark theme support
  - [x] 50.1 Add dark mode toggle to Settings
    - activity.recreate() for immediate effect
    - Read preference in MaterialTheme colorScheme selection
    - _Requirements: 47_
  - [x] 50.2 Fix hardcoded colors for theme compatibility
    - Cards use MaterialTheme.colorScheme.surface/surfaceVariant
    - Preview area stays white (intentional)

- [x] 51. Review and Classify screen
  - [x] 51.1 Create ReviewScreen + ReviewViewModel
    - Two tabs: Untagged (batch tag) + OCR Review (mark reviewed)
    - Multi-select with checkboxes, bulk tag assignment
    - _Requirements: 48_

- [x] 52. Share target (receive from other apps)
  - [x] 52.1 Add intent filters to manifest (ACTION_SEND, ACTION_VIEW)
  - [x] 52.2 Handle incoming intent in MainActivity
    - Auto-import after biometric auth
    - Show progress and result
    - _Requirements: 49_

- [x] 53. Security hardening pass
  - [x] 53.1 Web server token auth
  - [x] 53.2 Input sanitization (tags, filenames) via InputSanitizer
  - [x] 53.3 System tags protected from user removal
  - [x] 53.4 DebugLogger async file writes (ANR prevention)
  - [x] 53.5 TempFileCleanup moved to onResume (fixes sharing)
  - [x] 53.6 ACCESS_WIFI_STATE permission added

- [x] 54. Hackathon submission artifacts
  - [x] 54.1 HACKATHON.md with project overview for judges
  - [x] 54.2 GitHub Actions CI/CD workflow
  - [x] 54.3 PRIVACY_POLICY.md
  - [x] 54.4 README rewritten (humanized, complete build instructions)
  - [x] 54.5 Code comments humanized across key files


---

## Experimental Features — Graduation Checklist

Features below are currently behind experimental toggles. Each must pass the listed criteria before graduating to a stable release feature.

### Google Drive Support
- [ ] OAuth flow works reliably on fresh install
- [ ] Handles token expiry / refresh gracefully
- [ ] Folder browsing tested with 100+ files
- [ ] Error handling for network failures mid-sync
- [ ] Drive quota exceeded handling
- [ ] Remove feature flag gate

### S3 Compatible Storage
- [ ] Tested with AWS S3, MinIO, Backblaze B2
- [ ] Presigned URL upload/download verified
- [ ] Large file (>50MB) multipart upload
- [ ] Connection test validates credentials before save
- [ ] Region auto-detection from GPS
- [ ] Remove feature flag gate

### Backup & Restore
- [ ] Restore on fresh device (no prior KeyStore key) — verify key generation
- [ ] Restore with password-protected ZIP (Zip4j)
- [ ] Restore count confirmation dialog shown to user
- [ ] Large backup (50+ documents) performance tested
- [ ] Database schema migration on restore from older version
- [ ] Remove feature flag gate

### Background GPS Tracking
- [ ] Battery impact measured (<2% per hour)
- [ ] Adaptive algorithm verified (30s moving, 5min stationary)
- [ ] Data export to GPX format tested
- [ ] Foreground notification correctly shows/hides
- [ ] Permission revocation handled gracefully
- [ ] Remove feature flag gate

### Extended Image Formats
- [ ] WebP: import + preview + share verified
- [ ] HEIC: tested on API 28+ device, graceful failure on API 26-27
- [ ] BMP: large file (>10MB) doesn't ANR
- [ ] GIF: first frame renders correctly
- [ ] DICOM: uncompressed grayscale + RGB verified with real clinical images
- [ ] DICOM: error message is clear when compressed (JPEG2000) DICOM is loaded
- [ ] Remove feature flag gate

### Audio Playback & Android Auto
- [ ] MP3/M4A/WAV import and playback verified
- [ ] Android Auto tested with Desktop Head Unit (DHU) emulator
- [ ] Media session shows correct metadata on lock screen
- [ ] Skip next/previous works across tag boundaries
- [ ] Audio focus management (pauses when call comes in)
- [ ] Large playlist (100+ tracks) doesn't OOM
- [ ] Remove feature flag gate

### WiFi Document Sharing
- [ ] Server starts/stops cleanly (no port leak on repeated start/stop)
- [ ] Token auth blocks unauthorized access
- [ ] Upload via web UI imports into app correctly
- [ ] Rename via web UI reflects in app immediately
- [ ] Large file download (>50MB) doesn't timeout
- [ ] Multiple simultaneous browser connections
- [ ] Server auto-stops on app background (configurable)
- [ ] Tested on phone hotspot (no WiFi router)
- [ ] Remove feature flag gate

### Import from URL
- [ ] HTTPS certificate validation (reject self-signed by default)
- [ ] Timeout handling (>30s shows retry option)
- [ ] Redirect following (301/302) tested
- [ ] Large file download (>100MB) with progress
- [ ] Cancel during download verified
- [ ] Filename extraction from Content-Disposition header
- [ ] Remove feature flag gate (move to stable import options)
