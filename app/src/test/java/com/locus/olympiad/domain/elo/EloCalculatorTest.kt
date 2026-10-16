package com.locus.olympiad.domain.elo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EloCalculatorTest {

    @Test
    fun testExpectedScore_EqualRatings_ReturnsHalf() {
        val expected = EloCalculator.calculateExpectedScore(1000, 1000)
        assertEquals(0.5, expected, 0.0001)
    }

    @Test
    fun testExpectedScore_HigherRatedUser_HasAdvantage() {
        // User 1200 vs Question 1000
        val expected = EloCalculator.calculateExpectedScore(1200, 1000)
        // 1 / (1 + 10^(-200/400)) = 1 / (1 + 10^(-0.5)) ≈ 0.7597
        assertTrue(expected > 0.75 && expected < 0.77)
    }

    @Test
    fun testCalculateNewRating_CorrectSubmission_IncreasesRating() {
        val userElo = 1000
        val questionElo = 1000
        val result = EloCalculator.calculateNewRating(
            userElo = userElo,
            questionDifficulty = questionElo,
            isCorrect = true,
            customK = 32
        )
        // Delta = 32 * (1 - 0.5) = +16
        assertEquals(16, result.delta)
        assertEquals(1016, result.newElo)
        assertTrue(result.isCorrect)
    }

    @Test
    fun testCalculateNewRating_IncorrectSubmission_DecreasesRating() {
        val userElo = 1000
        val questionElo = 1000
        val result = EloCalculator.calculateNewRating(
            userElo = userElo,
            questionDifficulty = questionElo,
            isCorrect = false,
            customK = 32
        )
        // Delta = 32 * (0 - 0.5) = -16
        assertEquals(-16, result.delta)
        assertEquals(984, result.newElo)
    }

    @Test
    fun testMatchmakingWindow_WithinPlusMinus150Points() {
        val userElo = 1120
        val (minElo, maxElo) = EloCalculator.getMatchmakingWindow(userElo)
        assertEquals(970, minElo)
        assertEquals(1270, maxElo)
        assertEquals(300, maxElo - minElo)
    }
}
