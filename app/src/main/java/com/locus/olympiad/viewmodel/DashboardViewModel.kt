package com.locus.olympiad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.olympiad.repository.MatchmakingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    private val matchmakingRepository: MatchmakingRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = matchmakingRepository.getAllUserProfiles()
        .map { profiles ->
            val totalSolved = profiles.sumOf { it.questionsSolved }
            val avgElo = if (profiles.isNotEmpty()) {
                profiles.map { it.eloRating }.average().toInt()
            } else {
                1000
            }
            DashboardUiState(
                profiles = profiles,
                overallElo = avgElo,
                solvedCount = totalSolved,
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(isLoading = true)
        )
}
