package com.app.paperstow.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.app.paperstow.data.local.dao.DocumentDao
import com.app.paperstow.data.local.entity.DocumentEntity
import com.app.paperstow.domain.model.DocumentFormat
import com.app.paperstow.domain.model.DocumentType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RoomDatabaseInstrumentedTest {

    private lateinit var database: TravelDocsDatabase
    private lateinit var documentDao: DocumentDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TravelDocsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        documentDao = database.documentDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun writeAndReadDocument() = runBlocking {
        val doc = DocumentEntity(
            id = "test-doc-1",
            memberId = "default-member",
            type = DocumentType.PASSPORT.name,
            fileId = "vault-file-1",
            format = DocumentFormat.PDF.name,
            originalFileName = "my_passport.pdf",
            createdAt = 1000L,
            updatedAt = 1000L,
            extractionConfidence = 0.95f,
            requiresManualReview = false,
            ocrText = "Passport Sample"
        )
        documentDao.insert(doc)

        val retrieved = documentDao.getById("test-doc-1")
        assertNotNull(retrieved)
        assertEquals("my_passport.pdf", retrieved?.originalFileName)
        assertEquals(DocumentType.PASSPORT.name, retrieved?.type)
        assertEquals(1, documentDao.getCount("default-member"))

        documentDao.delete("test-doc-1")
        val afterDelete = documentDao.getById("test-doc-1")
        assertNull(afterDelete)
        assertEquals(0, documentDao.getCount("default-member"))
    }
}
