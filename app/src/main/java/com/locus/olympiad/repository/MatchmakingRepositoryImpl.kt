package com.locus.olympiad.repository

import com.locus.olympiad.data.dao.PracticeQuestionDao
import com.locus.olympiad.data.dao.UserTopicProfileDao
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.UserTopicProfile
import com.locus.olympiad.domain.elo.EloCalculationResult
import com.locus.olympiad.domain.elo.EloCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.abs

class MatchmakingRepositoryImpl(
    private val questionDao: PracticeQuestionDao,
    private val profileDao: UserTopicProfileDao
) : MatchmakingRepository {

    override fun getUserProfile(category: MathTopic): Flow<UserTopicProfile?> {
        return profileDao.getProfileByCategory(category.displayName)
    }

    override fun getAllUserProfiles(): Flow<List<UserTopicProfile>> {
        return profileDao.getAllProfiles()
    }

    override suspend fun getNextPracticeQuestion(category: MathTopic): PracticeQuestion? = withContext(Dispatchers.IO) {
        val categoryName = category.displayName
        
        // 1. Fetch current Elo in the selected category (default 1000)
        val userElo = profileDao.getEloRatingForCategory(categoryName) 
            ?: UserTopicProfile.DEFAULT_ELO

        // 2. Compute the +/- 150 points matchmaking window
        val (minElo, maxElo) = EloCalculator.getMatchmakingWindow(userElo)

        // 3. Query questions within +/- 150 rating points
        var question = questionDao.findQuestionInEloRange(categoryName, minElo, maxElo)

        // 4. Fallback if no questions are found in the exact window
        if (question == null) {
            question = questionDao.findClosestQuestion(categoryName, userElo)
        }

        question
    }

    override suspend fun submitAnswer(
        question: PracticeQuestion,
        submittedAnswer: String
    ): Pair<Boolean, EloCalculationResult> = withContext(Dispatchers.IO) {
        val categoryName = question.category
        val isCorrect = isAnswerCorrect(question.correctAnswer, submittedAnswer)

        // Fetch current user Elo
        val currentProfile = profileDao.getProfileByCategorySync(categoryName)
        val currentElo = currentProfile?.eloRating ?: UserTopicProfile.DEFAULT_ELO

        // Compute new Elo with standard logistic equation
        val eloResult = EloCalculator.calculateNewRating(
            userElo = currentElo,
            questionDifficulty = question.difficultyElo,
            isCorrect = isCorrect
        )

        // Persist the updated rating atomically
        val solvedIncrement = if (isCorrect) 1 else 0
        if (currentProfile == null) {
            profileDao.insertOrUpdateProfile(
                UserTopicProfile(
                    category = categoryName,
                    eloRating = eloResult.newElo,
                    questionsSolved = solvedIncrement,
                    questionsAttempted = 1
                )
            )
        } else {
            profileDao.recordSubmissionResult(
                category = categoryName,
                newElo = eloResult.newElo,
                solvedIncrement = solvedIncrement
            )
        }

        Pair(isCorrect, eloResult)
    }

    override fun isAnswerCorrect(correctAnswer: String, submittedAnswer: String): Boolean {
        val normalizedExpected = correctAnswer.trim().lowercase()
        val normalizedInput = submittedAnswer.trim().lowercase()

        // Direct equality
        if (normalizedExpected == normalizedInput) return true

        // Remove surrounding mathematical dollar signs or whitespace if present
        val strippedExpected = normalizedExpected.replace("$", "").trim()
        val strippedInput = normalizedInput.replace("$", "").trim()
        if (strippedExpected == strippedInput) return true

        // Check numerical / fractional equivalence (e.g., "84/5" == "16.8" or "3/1" == "3")
        try {
            val expectedVal = parseFractionOrDouble(strippedExpected)
            val inputVal = parseFractionOrDouble(strippedInput)
            if (expectedVal != null && inputVal != null) {
                return abs(expectedVal - inputVal) < 1e-6
            }
        } catch (_: Exception) {
            // Ignore parse failures and fall back to string comparison
        }

        return false
    }

    private fun parseFractionOrDouble(value: String): Double? {
        if (value.contains("/")) {
            val parts = value.split("/")
            if (parts.size == 2) {
                val numerator = parts[0].trim().toDoubleOrNull() ?: return null
                val denominator = parts[1].trim().toDoubleOrNull() ?: return null
                if (denominator != 0.0) return numerator / denominator
            }
        }
        return value.toDoubleOrNull()
    }
}
