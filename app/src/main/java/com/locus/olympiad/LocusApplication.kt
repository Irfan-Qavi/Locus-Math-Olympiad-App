package com.locus.olympiad

import android.app.Application
import com.locus.olympiad.data.LocusDatabase
import com.locus.olympiad.data.UserPreferencesRepository
import com.locus.olympiad.network.AiTutorClient
import com.locus.olympiad.repository.AiTutorRepositoryImpl
import com.locus.olympiad.repository.MatchmakingRepositoryImpl
import com.locus.olympiad.viewmodel.ViewModelFactory
import com.locus.olympiad.worker.RemoteContentSyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LocusApplication : Application() {

    val database: LocusDatabase by lazy {
        LocusDatabase.getInstance(this)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val matchmakingRepository: MatchmakingRepositoryImpl by lazy {
        MatchmakingRepositoryImpl(
            questionDao = database.practiceQuestionDao(),
            profileDao = database.userTopicProfileDao()
        )
    }

    val aiTutorRepository: AiTutorRepositoryImpl by lazy {
        val apiKey = BuildConfig.LLM_API_KEY
        if (apiKey.isNotBlank()) {
            AiTutorClient.setApiKey(apiKey)
        }
        val apiService = AiTutorClient.create()
        AiTutorRepositoryImpl(apiService)
    }

    val viewModelFactory: ViewModelFactory by lazy {
        ViewModelFactory(
            matchmakingRepository = matchmakingRepository,
            aiTutorRepository = aiTutorRepository,
            userPreferencesRepository = userPreferencesRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        try {
            RemoteContentSyncWorker.enqueuePeriodicSync(this)
        } catch (e: Exception) {
            // Safe fallback if WorkManager initialization is deferred
        }
    }
}
