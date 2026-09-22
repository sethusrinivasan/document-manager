# Known issues and library behaviors

Current pins: Room 2.8.5, Zip4j 2.11.6, ML Kit text recognition 16.0.1. Package `com.app.paperstow`. Room file is still `traveldocs.db`.

## Room — WAL and file replacement

**Library:** `androidx.room:room-runtime`

Room uses WAL. Overwriting `traveldocs.db` while Room is open leaves the process on the old WAL/cache.

**What we do:** restore checkpoints and closes Room, deletes `.db-wal` / `.db-shm`, inspects the backup for a `documents` table, then writes the new file. The next DAO call opens a fresh connection.

## PdfRenderer — one page at a time

**Library:** `android.graphics.pdf.PdfRenderer`

Only one page may be open. Concurrent `openPage` throws.

**What we do:** render on a single-thread dispatcher. For OCR, the first four pages are drawn onto a **white** bitmap (`eraseColor(WHITE)`). A transparent bitmap renders as a black JPEG and ML Kit returns no text.

## DocumentFile — slow large folders

**Library:** `androidx.documentfile:documentfile`

`listFiles()` is one Binder call per child.

**What we do:** enumerate on `Dispatchers.IO` with a 500-file cap. UI shows folder-scan progress.

## Zip4j — optional password

**Library:** `net.lingala.zip4j:zip4j:2.11.6`

Blank password → unencrypted ZIP. Non-blank (min 4 chars) → AES-256. `isEncrypted` is true for protected archives; a wrong password may only fail when entries are read.

**What we do:** restore prompts only if the archive is encrypted. Wrong password is reported to the user. The ZIP contains **decrypted** document bytes so another phone can restore.

## ML Kit — Latin OCR

**Library:** `com.google.mlkit:text-recognition:16.0.1` with `TextRecognizerOptions.DEFAULT_OPTIONS` (Latin).

The Latin model is bundled. Play services may still refresh it, which needs network. If recognition fails, the file is stored and `requiresManualReview` is set. Settings → Rebuild search index retries extraction. Raw PDF bytes are not passed to ML Kit.
