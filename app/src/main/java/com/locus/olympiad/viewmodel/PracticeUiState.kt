package com.locus.olympiad.viewmodel

import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.PracticeSessionConfig

/**
 * UI State for the Olympiad Practice Screen.
 * Tracks current question, student Elo, progressive hint reveal count,
 * session configuration, session progress, and AI tutor stepping stones.
 */
data class PracticeUiState(
    val currentQuestion: PracticeQuestion? = null,
    val userElo: Int = 1000,
    val revealedHintsCount: Int = 0,
    val incorrectAttempts: List<String> = emptyList(),
    val isHardStuck: Boolean = false,
    val aiTutorHint: String? = null,

    // Session Configuration and Progress Tracking
    val sessionConfig: PracticeSessionConfig = PracticeSessionConfig(),
    val sessionQuestionIndex: Int = 1,
    val sessionSolvedCount: Int = 0,
    val sessionNetEloDelta: Int = 0,
    val isSessionCompleted: Boolean = false,

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

    val sessionProgressText: String
        get() {
            return if (sessionConfig.isEndless) {
                "Problem #$sessionQuestionIndex • Endless"
            } else {
                "Problem $sessionQuestionIndex of ${sessionConfig.questionCount}"
            }
        }
}
