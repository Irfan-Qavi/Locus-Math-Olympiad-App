package com.locus.olympiad.domain.ai

import com.locus.olympiad.data.model.PracticeQuestion

/**
 * Interface for AI Olympiad Tutor interactions.
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
}
