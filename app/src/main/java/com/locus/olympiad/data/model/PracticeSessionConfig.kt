package com.locus.olympiad.data.model

import java.io.Serializable

/**
 * Configuration payload for a targeted Olympiad practice session.
 * Configured in the Pre-Practice Menu.
 *
 * @param topic Specific MathTopic, or null for "MIX ALL TYPES".
 * @param difficultyTier "Adaptive", "Foundational", "Intermediate", "Olympiad", or "All".
 * @param questionCount Number of questions (5, 10, 15, or 0 for Endless).
 */
data class PracticeSessionConfig(
    val topic: MathTopic? = null,
    val difficultyTier: String = "Adaptive",
    val questionCount: Int = 0,
    val isRiskFree: Boolean = false
) : Serializable {

    val isEndless: Boolean
        get() = questionCount <= 0

    val topicDisplayName: String
        get() = topic?.displayName ?: "Mix All Types"

    val displaySummary: String
        get() {
            val countStr = if (isEndless) "Endless" else "$questionCount Problems"
            val riskStr = if (isRiskFree) " • Risk-Free" else ""
            return "$topicDisplayName • $difficultyTier • $countStr$riskStr"
        }
}
