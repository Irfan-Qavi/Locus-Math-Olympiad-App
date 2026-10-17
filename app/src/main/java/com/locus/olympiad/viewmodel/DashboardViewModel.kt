package com.locus.olympiad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.olympiad.data.UserPreferencesRepository
import com.locus.olympiad.repository.MatchmakingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val matchmakingRepository: MatchmakingRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        matchmakingRepository.getAllUserProfiles(),
        userPreferencesRepository.getUserName()
    ) { profiles, userName ->
        val totalSolved = profiles.sumOf { it.questionsSolved }
        val avgElo = if (profiles.isNotEmpty()) {
            profiles.map { it.eloRating }.average().toInt()
        } else {
            1000
        }
        DashboardUiState(
            userName = userName,
            profiles = profiles,
            overallElo = avgElo,
            solvedCount = totalSolved,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun hasCompletedOnboarding(): Boolean {
        return userPreferencesRepository.hasCompletedOnboarding()
    }

    fun setUserName(name: String) {
        userPreferencesRepository.setUserName(name)
    }
}
