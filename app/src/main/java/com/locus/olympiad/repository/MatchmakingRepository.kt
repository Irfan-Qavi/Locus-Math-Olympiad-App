package com.locus.olympiad.repository

import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.UserTopicProfile
import com.locus.olympiad.domain.elo.EloCalculationResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Olympiad question matchmaking and Elo progression.
 */
interface MatchmakingRepository {

    /**
     * Retrieves the user's current Elo profile for a specific math category.
     */
    fun getUserProfile(category: MathTopic): Flow<UserTopicProfile?>

    /**
     * Gets all category profiles for dashboard visualization.
     */
    fun getAllUserProfiles(): Flow<List<UserTopicProfile>>

    /**
     * Fetches the next practice question matched to the user's current Elo rating or configured difficulty.
     * If category is null, queries across all topics ("MIX ALL TYPES").
     */
    suspend fun getNextPracticeQuestion(
        category: MathTopic?,
        difficultyTier: String = "Adaptive"
    ): PracticeQuestion?

    /**
     * Retrieves a specific question by its database ID, used for session restoration.
     */
    suspend fun getQuestionById(questionId: Long): PracticeQuestion?

    /**
     * Validates a user's answer submission, computes new Elo using standard Elo math,
     * updates the database, and returns the calculation result.
     * 
     * @param question The question being answered.
     * @param submittedAnswer The user's input answer string.
     * @param isSemanticCorrect Optional AI-evaluated correctness for subjective short answers.
     * @param isRiskFree If true, Elo delta is strictly 0 and rating is unaffected.
     * @param isShowAnswerPenalty If true, applies 1.1x standard incorrect penalty for revealing the answer.
     * @return Pair of (isCorrect: Boolean, calculationResult: EloCalculationResult)
     */
    suspend fun submitAnswer(
        question: PracticeQuestion,
        submittedAnswer: String,
        isSemanticCorrect: Boolean? = null,
        isRiskFree: Boolean = false,
        isShowAnswerPenalty: Boolean = false
    ): Pair<Boolean, EloCalculationResult>

    /**
     * Normalizes and verifies whether the submitted answer matches the correct answer.
     * Handles integer equivalences, fractions (e.g. 84/5), and case-insensitive strings.
     */
    fun isAnswerCorrect(correctAnswer: String, submittedAnswer: String): Boolean
}
