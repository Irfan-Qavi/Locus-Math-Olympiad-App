package com.locus.olympiad.viewmodel

import com.locus.olympiad.data.model.PracticeQuestion

/**
 * UI State for the Olympiad Practice Screen.
 * Tracks current question, student Elo, progressive hint reveal count,
 * incorrect attempt history, hard-stuck flag, and the AI tutor conceptual hint.
 */
data class PracticeUiState(
    val currentQuestion: PracticeQuestion? = null,
    val userElo: Int = 1000,
    val revealedHintsCount: Int = 0,
    val incorrectAttempts: List<String> = emptyList(),
    val isHardStuck: Boolean = false,
    val aiTutorHint: String? = null,

    // Auxiliary UI State
    val isLoadingQuestion: Boolean = false,
    val isEvaluatingAnswer: Boolean = false,
    val isLoadingAiHint: Boolean = false,
    val lastSubmissionCorrect: Boolean? = null,
    val lastEloDelta: Int? = null,
    val userDraftAnswer: String = "",
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
) {
    val totalHintsCount: Int
        get() = currentQuestion?.listOfHints?.size ?: 0

    val hasMoreHints: Boolean
        get() = revealedHintsCount < totalHintsCount

    val visibleHints: List<String>
        get() = currentQuestion?.listOfHints?.take(revealedHintsCount) ?: emptyList()
}
