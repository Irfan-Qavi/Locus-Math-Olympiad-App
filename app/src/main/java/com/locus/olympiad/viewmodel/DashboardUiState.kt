package com.locus.olympiad.viewmodel

import com.locus.olympiad.data.model.UserTopicProfile

data class DashboardUiState(
    val userName: String = "Olympiad Aspirant",
    val profiles: List<UserTopicProfile> = emptyList(),
    val overallElo: Int = 1000,
    val solvedCount: Int = 0,
    val streakDays: Int = 18,
    val activeSessionTopic: String = "Combinatorics",
    val activeSessionPreview: String = "Let G = (V, E) be a connected simple graph...",
    val isLoading: Boolean = false
) {
    val rankTitle: String
        get() = when {
            overallElo < 1050 -> "Candidate Master"
            overallElo < 1150 -> "Specialist"
            overallElo < 1250 -> "International Master"
            else -> "Grandmaster"
        }

    val userInitial: String
        get() = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "O"

    val algebraElo: Int
        get() = profiles.firstOrNull { it.category == "Algebra" }?.eloRating ?: 1000

    val geometryElo: Int
        get() = profiles.firstOrNull { it.category == "Geometry" }?.eloRating ?: 1000

    val combinatoricsElo: Int
        get() = profiles.firstOrNull { it.category == "Combinatorics" }?.eloRating ?: 1000

    val numberTheoryElo: Int
        get() = profiles.firstOrNull { it.category == "Number Theory" }?.eloRating ?: 1000
}
