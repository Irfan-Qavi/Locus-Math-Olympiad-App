package com.locus.olympiad.domain.ai

import com.locus.olympiad.data.model.PracticeQuestion

/**
 * Interface for AI Olympiad evaluation of student submissions.
 */
interface AiTutorRepository {

    /**
     * Evaluates whether the candidate's subjective response is mathematically and semantically equivalent
     * to the official question solution key (e.g. "not possible" vs "impossible", or equivalent algebra).
     */
    suspend fun evaluateSubjectiveAnswer(
        question: PracticeQuestion,
        candidateAnswer: String
    ): Result<Boolean>
}
