package com.locus.olympiad.viewmodel

import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.PracticeSessionConfig

/**
 * UI State for the Olympiad Practice Screen.
 * Tracks current question, student Elo rating, session progress,
 * and answer evaluation feedback.
 */
data class PracticeUiState(
    val currentQuestion: PracticeQuestion? = null,
    val userElo: Int = 1000,
    val incorrectAttempts: List<String> = emptyList(),

    // Session Configuration and Progress Tracking
    val sessionConfig: PracticeSessionConfig = PracticeSessionConfig(),
    val sessionQuestionIndex: Int = 1,
    val sessionSolvedCount: Int = 0,
    val sessionNetEloDelta: Int = 0,
    val isSessionCompleted: Boolean = false,

    // Auxiliary UI State
    val isLoadingQuestion: Boolean = false,
    val isEvaluatingAnswer: Boolean = false,
    val lastSubmissionCorrect: Boolean? = null,
    val lastEloDelta: Int? = null,
    val userDraftAnswer: String = "",
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val isProofSolutionRevealed: Boolean = false,
    val isAnswerRevealed: Boolean = false
) {
    val sessionProgressText: String
        get() {
            val riskStr = if (sessionConfig.isRiskFree) " [Risk-Free]" else ""
            return if (sessionConfig.isEndless) {
                "Problem #$sessionQuestionIndex • Endless$riskStr"
            } else {
                "Problem $sessionQuestionIndex of ${sessionConfig.questionCount}$riskStr"
            }
        }
}
