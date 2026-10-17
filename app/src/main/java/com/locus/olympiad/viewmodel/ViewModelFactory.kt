package com.locus.olympiad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.locus.olympiad.data.UserPreferencesRepository
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.repository.MatchmakingRepository

class ViewModelFactory(
    private val matchmakingRepository: MatchmakingRepository,
    private val aiTutorRepository: AiTutorRepository,
    private val userPreferencesRepository: UserPreferencesRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(PracticeViewModel::class.java) -> {
                PracticeViewModel(matchmakingRepository, aiTutorRepository) as T
            }
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> {
                if (userPreferencesRepository != null) {
                    DashboardViewModel(matchmakingRepository, userPreferencesRepository) as T
                } else {
                    throw IllegalStateException("UserPreferencesRepository required for DashboardViewModel")
                }
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
