# Known Issues & Library Behaviors

## Room Database — WAL Mode and File Replacement

**Library:** `androidx.room:room-runtime:2.6.1`
**Behavior:** Room uses WAL (Write-Ahead Logging) mode by default. When you overwrite `traveldocs.db` directly on disk while Room has an active connection, Room continues reading from its in-memory cache and the WAL file — ignoring the new database content.

**Our fix:** Before overwriting the `.db` file during restore, we:
1. Call `db.close()` on the Room instance (triggers WAL checkpoint and releases file handles)
2. Delete `.db-wal` and `.db-shm` files
3. Write the new database file

After this, Room's next DAO query opens a fresh connection to the restored database.

**Potential upstream improvement:** Room could provide an API like `invalidateAndReopen()` that safely checkpoints, closes, and reopens the database from the current file on disk. Today this requires manual connection management.

## PdfRenderer — Single Page Open Constraint

**Library:** `android.graphics.pdf.PdfRenderer` (Android framework, API 21+)
**Behavior:** `PdfRenderer` can only have one page open at a time. Calling `openPage(n)` while another page is still open throws `IllegalStateException`. This is a fundamental limitation of the underlying PDFium library.

**Our fix:** All page rendering runs on a dedicated `newSingleThreadExecutor` coroutine dispatcher. Pages are opened, rendered, and closed sequentially — never concurrently.

**Impact:** PDF scrolling through large documents renders pages one at a time. Users may see placeholder shimmer for pages not yet rendered. This is acceptable UX for the security benefit of not caching all pages in memory.

## DocumentFile — Slow IPC for Large Folders

**Library:** `androidx.documentfile:documentfile` (SAF/Storage Access Framework)
**Behavior:** `DocumentFile.listFiles()` issues one Binder IPC call per file. For folders with 500+ files, this takes several seconds and blocks the calling thread.

**Our fix:** All folder enumeration runs on `Dispatchers.IO`. The UI shows "Scanning folder..." with an indeterminate progress indicator while enumeration happens in the background.

**Potential upstream improvement:** A batch `listFiles()` API that returns all children in a single IPC call would eliminate this bottleneck.

## Zip4j — Password Validation Timing

**Library:** `net.lingala.zip4j:zip4j:2.11.5`
**Behavior:** Zip4j only validates the password when actually extracting file content. `ZipFile.isEncrypted` returns true for password-protected ZIPs, but passing a wrong password may not throw until bytes are read.

**Our fix:** We detect `isEncrypted` early and prompt for password before extraction. If extraction fails with a wrong password, we catch the exception and report "Incorrect password" to the user.

## ML Kit — Model Download on First Use

**Library:** `com.google.mlkit:text-recognition:16.0.0`
**Behavior:** ML Kit downloads its OCR model on first use (~20MB). If the device is offline during the first import, OCR silently fails and the document is stored without metadata.

**Our fix:** Graceful degradation — if OCR fails for any reason, the document is still stored and tagged as `requiresManualReview = true`. Users can classify it later via the Review & Classify screen.
