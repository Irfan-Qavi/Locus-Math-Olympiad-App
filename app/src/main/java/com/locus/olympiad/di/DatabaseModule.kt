package com.locus.olympiad.di

import android.content.Context
import com.locus.olympiad.data.LocusDatabase
import com.locus.olympiad.data.dao.PracticeQuestionDao
import com.locus.olympiad.data.dao.TheoryModuleDao
import com.locus.olympiad.data.dao.UserTopicProfileDao
import com.locus.olympiad.repository.MatchmakingRepository
import com.locus.olympiad.repository.MatchmakingRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for providing Room database instances, DAOs, and MatchmakingRepository.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLocusDatabase(
        @ApplicationContext context: Context
    ): LocusDatabase {
        return LocusDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideUserTopicProfileDao(database: LocusDatabase): UserTopicProfileDao {
        return database.userTopicProfileDao()
    }

    @Provides
    @Singleton
    fun providePracticeQuestionDao(database: LocusDatabase): PracticeQuestionDao {
        return database.practiceQuestionDao()
    }

    @Provides
    @Singleton
    fun provideTheoryModuleDao(database: LocusDatabase): TheoryModuleDao {
        return database.theoryModuleDao()
    }

    @Provides
    @Singleton
    fun provideMatchmakingRepository(
        questionDao: PracticeQuestionDao,
        profileDao: UserTopicProfileDao
    ): MatchmakingRepository {
        return MatchmakingRepositoryImpl(questionDao, profileDao)
    }
}
