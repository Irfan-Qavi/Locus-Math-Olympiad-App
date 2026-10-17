package com.locus.olympiad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.locus.olympiad.data.UserPreferencesRepository
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.repository.MatchmakingRepository

class ViewModelFactory(
    private val matchmakingRepository: MatchmakingRepository,
    private val aiTutorRepository: AiTutorRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PracticeViewModel::class.java) -> {
                PracticeViewModel(
                    matchmakingRepository = matchmakingRepository,
                    aiTutorRepository = aiTutorRepository,
                    userPreferencesRepository = userPreferencesRepository
                ) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                DashboardViewModel(
                    matchmakingRepository = matchmakingRepository,
                    userPreferencesRepository = userPreferencesRepository
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
