package com.app.traveldocs.domain.properties

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import com.app.traveldocs.data.importer.DocumentFormatValidator
import com.app.traveldocs.data.importer.FileDocumentImporter
import com.app.traveldocs.domain.model.DocumentFormat
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.byte
import io.kotest.property.arbitrary.byteArray
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertTrue

/**
 * Property 6: Supported format import acceptance
 *
 * For any file with a format in {PDF, JPG, PNG} and valid content (correct magic bytes),
 * the import operation should succeed without error.
 *
 * **Validates: Requirements 3.6**
 */
@DisplayName("Property 6: Supported format import acceptance")
@JUnitTag("Feature: travel-document-manager, Property 6: Supported format import acceptance")
class SupportedFormatImportAcceptancePropertyTest {

    private lateinit var context: Context
    private lateinit var contentResolver: ContentResolver
    private lateinit var formatValidator: DocumentFormatValidator
    private lateinit var importer: FileDocumentImporter

    companion object {
        // Magic bytes for each supported format
        private val PDF_MAGIC = byteArrayOf(0x25, 0x50, 0x44, 0x46) // %PDF
        private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // .PNG

        // MIME types for each format
        private val FORMAT_TO_MIME = mapOf(
            DocumentFormat.PDF to "application/pdf",
            DocumentFormat.JPG to "image/jpeg",
            DocumentFormat.PNG to "image/png"
        )

        // File extensions for each format
        private val FORMAT_TO_EXTENSION = mapOf(
            DocumentFormat.PDF to "pdf",
            DocumentFormat.JPG to "jpg",
            DocumentFormat.PNG to "png"
        )
    }

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        formatValidator = DocumentFormatValidator()

        every { context.contentResolver } returns contentResolver

        importer = FileDocumentImporter(context, formatValidator)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Returns the correct magic bytes for the given format.
     */
    private fun magicBytesFor(format: DocumentFormat): ByteArray {
        return when (format) {
            DocumentFormat.PDF -> PDF_MAGIC
            DocumentFormat.JPG -> JPEG_MAGIC
            DocumentFormat.PNG -> PNG_MAGIC
        }
    }

    /**
     * Generator for file content with valid magic bytes for a given format,
     * followed by random content bytes.
     */
    private fun arbValidFileContent(format: DocumentFormat): Arb<ByteArray> = arbitrary { rs ->
        val magic = magicBytesFor(format)
        val randomContentSize = Arb.int(1..1024).bind()
        val randomContent = Arb.byteArray(Arb.int(randomContentSize..randomContentSize), Arb.byte()).bind()
        magic + randomContent
    }

    /**
     * Generator that produces a supported format together with valid file bytes.
     */
    private val arbFormatAndContent: Arb<Pair<DocumentFormat, ByteArray>> = arbitrary {
        val format = Arb.enum<DocumentFormat>().bind()
        val content = arbValidFileContent(format).bind()
        format to content
    }

    /**
     * Sets up mocks for contentResolver to return the given bytes, MIME type, and filename.
     */
    private fun setupMocksForFile(
        uri: Uri,
        bytes: ByteArray,
        mimeType: String,
        fileName: String
    ) {
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(bytes)
        every { contentResolver.getType(uri) } returns mimeType

        val cursor = mockk<Cursor>(relaxed = true)
        every { contentResolver.query(uri, null, null, null, null) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) } returns 0
        every { cursor.getString(0) } returns fileName
        every { cursor.close() } returns Unit
    }

    @Test
    @DisplayName("Any file with valid magic bytes for PDF, JPG, or PNG is imported successfully")
    fun `any file with valid magic bytes for supported format imports successfully`() = runTest {
        checkAll(100, arbFormatAndContent) { (format, content) ->
            val uri = mockk<Uri>(relaxed = true)
            val mimeType = FORMAT_TO_MIME[format]!!
            val extension = FORMAT_TO_EXTENSION[format]!!
            val fileName = "document.$extension"

            setupMocksForFile(uri, content, mimeType, fileName)

            val result = importer.importFromFile(uri)

            assertTrue(
                result.isSuccess,
                "Import should succeed for format $format with valid magic bytes, " +
                    "but failed with: ${result.exceptionOrNull()?.message}"
            )

            val imported = result.getOrThrow()
            assertTrue(
                imported.rawBytes.contentEquals(content),
                "Imported bytes should match original content"
            )
            assertTrue(
                imported.format == format,
                "Detected format should be $format, but was ${imported.format}"
            )
        }
    }

    @Test
    @DisplayName("PDF files with valid %PDF magic bytes are always accepted")
    fun `pdf files with valid magic bytes are always accepted`() = runTest {
        checkAll(100, arbValidFileContent(DocumentFormat.PDF)) { content ->
            val uri = mockk<Uri>(relaxed = true)
            setupMocksForFile(uri, content, "application/pdf", "test.pdf")

            val result = importer.importFromFile(uri)

            assertTrue(
                result.isSuccess,
                "PDF import should succeed with valid magic bytes, " +
                    "but failed with: ${result.exceptionOrNull()?.message}"
            )
            assertTrue(
                result.getOrThrow().format == DocumentFormat.PDF,
                "Detected format should be PDF"
            )
        }
    }

    @Test
    @DisplayName("JPG files with valid FF D8 FF magic bytes are always accepted")
    fun `jpg files with valid magic bytes are always accepted`() = runTest {
        checkAll(100, arbValidFileContent(DocumentFormat.JPG)) { content ->
            val uri = mockk<Uri>(relaxed = true)
            setupMocksForFile(uri, content, "image/jpeg", "photo.jpg")

            val result = importer.importFromFile(uri)

            assertTrue(
                result.isSuccess,
                "JPG import should succeed with valid magic bytes, " +
                    "but failed with: ${result.exceptionOrNull()?.message}"
            )
            assertTrue(
                result.getOrThrow().format == DocumentFormat.JPG,
                "Detected format should be JPG"
            )
        }
    }

    @Test
    @DisplayName("PNG files with valid 89 50 4E 47 magic bytes are always accepted")
    fun `png files with valid magic bytes are always accepted`() = runTest {
        checkAll(100, arbValidFileContent(DocumentFormat.PNG)) { content ->
            val uri = mockk<Uri>(relaxed = true)
            setupMocksForFile(uri, content, "image/png", "image.png")

            val result = importer.importFromFile(uri)

            assertTrue(
                result.isSuccess,
                "PNG import should succeed with valid magic bytes, " +
                    "but failed with: ${result.exceptionOrNull()?.message}"
            )
            assertTrue(
                result.getOrThrow().format == DocumentFormat.PNG,
                "Detected format should be PNG"
            )
        }
    }

    @Test
    @DisplayName("Valid files succeed regardless of random content after magic bytes")
    fun `valid files succeed regardless of trailing content size`() = runTest {
        val arbLargeContent: Arb<Pair<DocumentFormat, ByteArray>> = arbitrary {
            val format = Arb.enum<DocumentFormat>().bind()
            val magic = magicBytesFor(format)
            // Generate varying sizes from minimal to large
            val randomContentSize = Arb.int(1..4096).bind()
            val randomContent = Arb.byteArray(Arb.int(randomContentSize..randomContentSize), Arb.byte()).bind()
            format to (magic + randomContent)
        }

        checkAll(100, arbLargeContent) { (format, content) ->
            val uri = mockk<Uri>(relaxed = true)
            val mimeType = FORMAT_TO_MIME[format]!!
            val extension = FORMAT_TO_EXTENSION[format]!!
            setupMocksForFile(uri, content, mimeType, "file.$extension")

            val result = importer.importFromFile(uri)

            assertTrue(
                result.isSuccess,
                "Import should succeed for $format regardless of content size (${content.size} bytes), " +
                    "but failed with: ${result.exceptionOrNull()?.message}"
            )
        }
    }

    @Test
    @DisplayName("Import succeeds with MIME type alone when magic bytes are valid")
    fun `import succeeds with mime type when magic bytes match`() = runTest {
        checkAll(100, arbFormatAndContent) { (format, content) ->
            val uri = mockk<Uri>(relaxed = true)
            val mimeType = FORMAT_TO_MIME[format]!!

            // Set up with MIME type but no filename cursor result
            every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(content)
            every { contentResolver.getType(uri) } returns mimeType

            val cursor = mockk<Cursor>(relaxed = true)
            every { contentResolver.query(uri, null, null, null, null) } returns cursor
            every { cursor.moveToFirst() } returns false
            every { cursor.close() } returns Unit

            every { uri.lastPathSegment } returns null

            val result = importer.importFromFile(uri)

            assertTrue(
                result.isSuccess,
                "Import should succeed with valid MIME type and magic bytes for $format, " +
                    "but failed with: ${result.exceptionOrNull()?.message}"
            )
        }
    }
}
