package com.app.paperstow.di

import android.content.Context
import androidx.room.Room
import com.app.paperstow.data.local.DocumentRepositoryImpl
import com.app.paperstow.data.local.SearchEngineImpl
import com.app.paperstow.data.local.TagRepositoryImpl
import com.app.paperstow.data.local.TravelDocsDatabase
import com.app.paperstow.data.local.auth.AuthRepositoryImpl
import com.app.paperstow.data.local.auth.AuthSessionManager
import com.app.paperstow.data.local.dao.DocumentDao
import com.app.paperstow.data.local.dao.DocumentMetadataDao
import com.app.paperstow.data.local.dao.DocumentTagDao
import com.app.paperstow.data.local.dao.FamilyMemberDao
import com.app.paperstow.data.local.dao.GpsTrackDao
import com.app.paperstow.data.local.storage.DocumentFileStorageImpl
import com.app.paperstow.data.nlp.BasicDocumentChecklistGenerator
import com.app.paperstow.data.nlp.RegexNaturalLanguageParser
import com.app.paperstow.data.scanner.MlKitMetadataExtractor
import com.app.paperstow.data.tags.AutoTagGeneratorImpl
import com.app.paperstow.domain.repository.AuthRepository
import com.app.paperstow.domain.repository.AutoTagGenerator
import com.app.paperstow.domain.repository.DocumentChecklistGenerator
import com.app.paperstow.domain.repository.DocumentFileStorage
import com.app.paperstow.domain.repository.DocumentRepository
import com.app.paperstow.domain.repository.MetadataExtractor
import com.app.paperstow.domain.repository.NaturalLanguageParser
import com.app.paperstow.domain.repository.SearchEngine
import com.app.paperstow.domain.repository.SessionManager
import com.app.paperstow.domain.repository.TagRepository
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
        ).addMigrations(TravelDocsDatabase.MIGRATION_2_3, TravelDocsDatabase.MIGRATION_3_4).build()
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
