package com.locus.olympiad.domain.ai

import com.locus.olympiad.data.model.PracticeQuestion

/**
 * Interface for AI Olympiad Tutor interactions and semantic evaluation.
 */
interface AiTutorRepository {

    /**
     * Packages the question, user Elo, and incorrect attempts into the Olympiad coach prompt
     * and requests a single-sentence conceptual hint from the LLM.
     */
    suspend fun getConceptualHint(
        question: PracticeQuestion,
        userElo: Int,
        incorrectAttempts: List<String>
    ): Result<String>

    /**
     * Evaluates whether the candidate's subjective response is semantically equivalent
     * to the official question solution key (e.g. "not possible" vs "impossible", or equivalent algebra).
     */
    suspend fun evaluateSubjectiveAnswer(
        question: PracticeQuestion,
        candidateAnswer: String
    ): Result<Boolean>
}
