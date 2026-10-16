package com.locus.olympiad.domain.elo

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Result of an Elo rating calculation.
 */
data class EloCalculationResult(
    val oldElo: Int,
    val newElo: Int,
    val delta: Int,
    val expectedScore: Double,
    val isCorrect: Boolean,
    val questionDifficulty: Int
)

/**
 * Standard Elo matchmaking and rating engine for Math Olympiad preparation.
 * Uses logistic curve expectation: E = 1 / (1 + 10^((R_b - R_a) / 400)).
 */
object EloCalculator {

    const val DEFAULT_K_FACTOR = 32
    const val MINIMUM_ELO = 100
    const val ELO_MATCHMAKING_WINDOW = 150 // +/- 150 points

    /**
     * Calculates the expected probability of the user answering the question correctly.
     * @param userElo The current Elo rating of the student.
     * @param questionDifficulty The Elo difficulty rating of the question.
     * @return Expected score between 0.0 and 1.0.
     */
    fun calculateExpectedScore(userElo: Int, questionDifficulty: Int): Double {
        val exponent = (questionDifficulty - userElo).toDouble() / 400.0
        return 1.0 / (1.0 + 10.0.pow(exponent))
    }

    /**
     * Determines appropriate K-factor based on competitive Olympiad tier.
     */
    fun getKFactor(userElo: Int): Int {
        return when {
            userElo < 1400 -> 32 // Rapid skill calibration for developing students
            userElo < 2100 -> 24 // Intermediate / Tier II & III
            else -> 16           // Master / International Olympiad level
        }
    }

    /**
     * Calculates the updated Elo score following a submission.
     * 
     * @param userElo The user's rating before the submission.
     * @param questionDifficulty The question's difficulty Elo.
     * @param isCorrect Whether the submission was correct.
     * @param customK Optional override for K-factor.
     */
    fun calculateNewRating(
        userElo: Int,
        questionDifficulty: Int,
        isCorrect: Boolean,
        customK: Int? = null
    ): EloCalculationResult {
        val expected = calculateExpectedScore(userElo, questionDifficulty)
        val actual = if (isCorrect) 1.0 else 0.0
        val k = customK ?: getKFactor(userElo)

        val deltaRaw = k * (actual - expected)
        val delta = deltaRaw.roundToInt()

        // Ensure rating does not drop below MINIMUM_ELO
        val newElo = (userElo + delta).coerceAtLeast(MINIMUM_ELO)

        return EloCalculationResult(
            oldElo = userElo,
            newElo = newElo,
            delta = newElo - userElo,
            expectedScore = expected,
            isCorrect = isCorrect,
            questionDifficulty = questionDifficulty
        )
    }

    /**
     * Computes the matchmaking difficulty window [minElo, maxElo] for a user.
     */
    fun getMatchmakingWindow(userElo: Int): Pair<Int, Int> {
        val min = (userElo - ELO_MATCHMAKING_WINDOW).coerceAtLeast(MINIMUM_ELO)
        val max = userElo + ELO_MATCHMAKING_WINDOW
        return Pair(min, max)
    }
}
