package com.locus.olympiad

import android.app.Application
import com.locus.olympiad.data.LocusDatabase
import com.locus.olympiad.network.AiTutorClient
import com.locus.olympiad.repository.AiTutorRepositoryImpl
import com.locus.olympiad.repository.MatchmakingRepositoryImpl
import com.locus.olympiad.viewmodel.ViewModelFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LocusApplication : Application() {

    val database: LocusDatabase by lazy {
        LocusDatabase.getInstance(this)
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
            aiTutorRepository = aiTutorRepository
        )
    }
}
