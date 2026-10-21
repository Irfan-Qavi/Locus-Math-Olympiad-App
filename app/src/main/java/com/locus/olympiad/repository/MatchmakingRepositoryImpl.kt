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

    override suspend fun getNextPracticeQuestion(
        category: MathTopic?,
        difficultyTier: String,
        solvedIds: List<Long>
    ): PracticeQuestion? = withContext(Dispatchers.IO) {
        val categoryName = category?.displayName

        val (minElo, maxElo, targetElo) = when (difficultyTier.lowercase()) {
            "novice", "foundational" -> Triple(0, 1199, 1000)
            "intermediate" -> Triple(1200, 1500, 1350)
            "olympiad", "advanced" -> Triple(1501, 3000, 1750)
            "all", "all difficulties" -> Triple(0, 3000, 1300)
            else -> {
                // Adaptive mode: match user's actual Elo (+/- 150)
                val userElo = if (categoryName != null) {
                    profileDao.getEloRatingForCategory(categoryName) ?: UserTopicProfile.DEFAULT_ELO
                } else {
                    val all = profileDao.getAllProfilesSync()
                    if (all.isNotEmpty()) all.map { it.eloRating }.average().toInt() else UserTopicProfile.DEFAULT_ELO
                }
                val (windowMin, windowMax) = EloCalculator.getMatchmakingWindow(userElo)
                Triple(windowMin, windowMax, userElo)
            }
        }

        val effectiveSolvedIds = if (solvedIds.isEmpty()) listOf(-1L) else solvedIds

        var questions = questionDao.findQuestionsFiltered(categoryName, minElo, maxElo, effectiveSolvedIds)
        if (questions.isEmpty()) {
            questions = questionDao.findClosestQuestionsFiltered(categoryName, targetElo, effectiveSolvedIds)
        }
        if (questions.isEmpty() && categoryName != null) {
            // Fallback to any topic if the specific category had no questions
            questions = questionDao.findClosestQuestionsFiltered(null, targetElo, effectiveSolvedIds)
        }

        questions.firstOrNull()
    }

    override suspend fun getQuestionById(questionId: Long): PracticeQuestion? = withContext(Dispatchers.IO) {
        questionDao.getQuestionByIdSync(questionId)
    }

    override suspend fun submitAnswer(
        question: PracticeQuestion,
        submittedAnswer: String,
        isSemanticCorrect: Boolean?,
        isRiskFree: Boolean,
        isShowAnswerPenalty: Boolean
    ): Pair<Boolean, EloCalculationResult> = withContext(Dispatchers.IO) {
        val categoryName = question.category

        // Fetch current user Elo
        val currentProfile = profileDao.getProfileByCategorySync(categoryName)
        val currentElo = currentProfile?.eloRating ?: UserTopicProfile.DEFAULT_ELO

        // Subjective Proof questions: Neutral Elo (strictly 0 delta)
        if (question.resolvedType == com.locus.olympiad.data.model.QuestionType.SUBJECTIVE_PROOF) {
            val isUserValidated = submittedAnswer.equals("PROVED", ignoreCase = true) ||
                    submittedAnswer.equals("SOLVED", ignoreCase = true) ||
                    submittedAnswer.equals("VALID", ignoreCase = true)
            val solvedIncrement = if (isUserValidated) 1 else 0

            if (currentProfile == null) {
                profileDao.insertOrUpdateProfile(
                    UserTopicProfile(
                        category = categoryName,
                        eloRating = currentElo,
                        questionsSolved = solvedIncrement,
                        questionsAttempted = 1
                    )
                )
            } else {
                profileDao.recordSubmissionResult(
                    category = categoryName,
                    newElo = currentElo,
                    solvedIncrement = solvedIncrement
                )
            }

            return@withContext Pair(
                isUserValidated,
                EloCalculationResult(
                    oldElo = currentElo,
                    newElo = currentElo,
                    delta = 0,
                    expectedScore = EloCalculator.calculateExpectedScore(currentElo, question.difficultyElo),
                    isCorrect = isUserValidated,
                    questionDifficulty = question.difficultyElo
                )
            )
        }

        // Determine correctness
        val isCorrect = if (isShowAnswerPenalty) {
            false
        } else if (question.resolvedType == com.locus.olympiad.data.model.QuestionType.SUBJECTIVE_SHORT && isSemanticCorrect != null) {
            isSemanticCorrect
        } else {
            isAnswerCorrect(question.correctAnswer, submittedAnswer)
        }

        // Risk-Free Practice Mode: Elo delta is strictly 0 and rating does not change
        if (isRiskFree) {
            val solvedIncrement = if (isCorrect) 1 else 0
            if (currentProfile == null) {
                profileDao.insertOrUpdateProfile(
                    UserTopicProfile(
                        category = categoryName,
                        eloRating = currentElo,
                        questionsSolved = solvedIncrement,
                        questionsAttempted = 1
                    )
                )
            } else {
                profileDao.recordSubmissionResult(
                    category = categoryName,
                    newElo = currentElo,
                    solvedIncrement = solvedIncrement
                )
            }

            return@withContext Pair(
                isCorrect,
                EloCalculationResult(
                    oldElo = currentElo,
                    newElo = currentElo,
                    delta = 0,
                    expectedScore = EloCalculator.calculateExpectedScore(currentElo, question.difficultyElo),
                    isCorrect = isCorrect,
                    questionDifficulty = question.difficultyElo
                )
            )
        }

        // Compute new Elo with standard logistic equation or 1.1x surrender penalty
        val eloResult = if (isShowAnswerPenalty) {
            val standardPenaltyResult = EloCalculator.calculateNewRating(
                userElo = currentElo,
                questionDifficulty = question.difficultyElo,
                isCorrect = false
            )
            val penalty = kotlin.math.round(standardPenaltyResult.delta * 1.1).toInt()
            val newElo = maxOf(0, currentElo + penalty)
            EloCalculationResult(
                oldElo = currentElo,
                newElo = newElo,
                delta = penalty,
                expectedScore = standardPenaltyResult.expectedScore,
                isCorrect = false,
                questionDifficulty = question.difficultyElo
            )
        } else {
            EloCalculator.calculateNewRating(
                userElo = currentElo,
                questionDifficulty = question.difficultyElo,
                isCorrect = isCorrect
            )
        }

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
