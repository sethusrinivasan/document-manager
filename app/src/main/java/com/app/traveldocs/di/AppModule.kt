package com.app.traveldocs.di

import android.content.Context
import androidx.room.Room
import com.app.traveldocs.data.local.DocumentRepositoryImpl
import com.app.traveldocs.data.local.SearchEngineImpl
import com.app.traveldocs.data.local.TagRepositoryImpl
import com.app.traveldocs.data.local.TravelDocsDatabase
import com.app.traveldocs.data.local.auth.AuthRepositoryImpl
import com.app.traveldocs.data.local.auth.AuthSessionManager
import com.app.traveldocs.data.local.dao.DocumentDao
import com.app.traveldocs.data.local.dao.DocumentMetadataDao
import com.app.traveldocs.data.local.dao.DocumentTagDao
import com.app.traveldocs.data.local.dao.FamilyMemberDao
import com.app.traveldocs.data.local.dao.GpsTrackDao
import com.app.traveldocs.data.local.storage.DocumentFileStorageImpl
import com.app.traveldocs.data.nlp.BasicDocumentChecklistGenerator
import com.app.traveldocs.data.nlp.RegexNaturalLanguageParser
import com.app.traveldocs.data.scanner.MlKitMetadataExtractor
import com.app.traveldocs.data.tags.AutoTagGeneratorImpl
import com.app.traveldocs.domain.repository.AuthRepository
import com.app.traveldocs.domain.repository.AutoTagGenerator
import com.app.traveldocs.domain.repository.DocumentChecklistGenerator
import com.app.traveldocs.domain.repository.DocumentFileStorage
import com.app.traveldocs.domain.repository.DocumentRepository
import com.app.traveldocs.domain.repository.MetadataExtractor
import com.app.traveldocs.domain.repository.NaturalLanguageParser
import com.app.traveldocs.domain.repository.SearchEngine
import com.app.traveldocs.domain.repository.SessionManager
import com.app.traveldocs.domain.repository.TagRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds @Singleton abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindSessionManager(impl: AuthSessionManager): SessionManager
    @Binds @Singleton abstract fun bindDocumentFileStorage(impl: DocumentFileStorageImpl): DocumentFileStorage
    @Binds @Singleton abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
    @Binds @Singleton abstract fun bindSearchEngine(impl: SearchEngineImpl): SearchEngine
    @Binds @Singleton abstract fun bindNaturalLanguageParser(impl: RegexNaturalLanguageParser): NaturalLanguageParser
    @Binds @Singleton abstract fun bindChecklistGenerator(impl: BasicDocumentChecklistGenerator): DocumentChecklistGenerator
    @Binds @Singleton abstract fun bindMetadataExtractor(impl: MlKitMetadataExtractor): MetadataExtractor
    @Binds @Singleton abstract fun bindAutoTagGenerator(impl: AutoTagGeneratorImpl): AutoTagGenerator
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TravelDocsDatabase {
        return Room.databaseBuilder(
            context,
            TravelDocsDatabase::class.java,
            "traveldocs.db"
        ).build()
    }

    @Provides fun provideDocumentDao(db: TravelDocsDatabase): DocumentDao = db.documentDao()
    @Provides fun provideDocumentMetadataDao(db: TravelDocsDatabase): DocumentMetadataDao = db.documentMetadataDao()
    @Provides fun provideDocumentTagDao(db: TravelDocsDatabase): DocumentTagDao = db.documentTagDao()
    @Provides fun provideFamilyMemberDao(db: TravelDocsDatabase): FamilyMemberDao = db.familyMemberDao()
    @Provides fun provideGpsTrackDao(db: TravelDocsDatabase): GpsTrackDao = db.gpsTrackDao()

    @Provides
    @Singleton
    fun provideDocumentRepository(
        db: TravelDocsDatabase,
        documentDao: DocumentDao,
        metadataDao: DocumentMetadataDao,
        tagDao: DocumentTagDao
    ): DocumentRepository {
        return DocumentRepositoryImpl(db, documentDao, metadataDao, tagDao)
    }
}
