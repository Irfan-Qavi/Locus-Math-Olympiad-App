package com.locus.olympiad.di

import com.locus.olympiad.BuildConfig
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.network.AiTutorApiService
import com.locus.olympiad.network.AiTutorClient
import com.locus.olympiad.repository.AiTutorRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt DI module for providing network-level dependencies, including Retrofit AI tutor client.
 * API key is extracted securely from local.properties via BuildConfig.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAiTutorApiService(): AiTutorApiService {
        val apiKey = BuildConfig.LLM_API_KEY
        if (apiKey.isNotBlank()) {
            AiTutorClient.setApiKey(apiKey)
        }
        return AiTutorClient.create()
    }

    @Provides
    @Singleton
    fun provideAiTutorRepository(
        apiService: AiTutorApiService
    ): AiTutorRepository {
        return AiTutorRepositoryImpl(apiService)
    }
}
