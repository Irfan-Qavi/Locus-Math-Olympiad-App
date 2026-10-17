package com.locus.olympiad.repository

import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.domain.ai.AiTutorPromptBuilder
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.network.AiTutorApiService
import com.locus.olympiad.network.model.ChatCompletionRequest
import com.locus.olympiad.network.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiTutorRepositoryImpl(
    private val apiService: AiTutorApiService
) : AiTutorRepository {

    override suspend fun getConceptualHint(
        question: PracticeQuestion,
        userElo: Int,
        incorrectAttempts: List<String>
    ): Result<String> = withContext(Dispatchers.IO) {
        val prompt = AiTutorPromptBuilder.buildPrompt(
            userElo = userElo,
            category = question.category,
            latexContent = question.latexContent,
            incorrectAttempts = incorrectAttempts
        )

        try {
            val request = ChatCompletionRequest(
                messages = listOf(
                    ChatMessage(
                        role = "system",
                        content = "You are Locus, an elite International Math Olympiad coach. Always adhere strictly to the one-sentence conceptual hint rule."
                    ),
                    ChatMessage(
                        role = "user",
                        content = prompt
                    )
                )
            )

            val response = apiService.getChatCompletion(request)
            if (response.isSuccessful) {
                val hint = response.body()?.choices?.firstOrNull()?.message?.content?.trim()
                if (!hint.isNullOrBlank()) {
                    return@withContext Result.success(hint)
                }
            }

            Result.success(generateOfflineSteppingStone(question, incorrectAttempts))
        } catch (e: Exception) {
            Result.success(generateOfflineSteppingStone(question, incorrectAttempts))
        }
    }

    override suspend fun evaluateSubjectiveAnswer(
        question: PracticeQuestion,
        candidateAnswer: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmedCandidate = candidateAnswer.trim()
        val trimmedCorrect = question.correctAnswer.trim()

        // 1. Direct equivalence fast-path
        if (trimmedCandidate.equals(trimmedCorrect, ignoreCase = true)) {
            return@withContext Result.success(true)
        }

        // 2. Build prompt for LLM semantic evaluation
        val evaluationPrompt = """
            You are an elite International Mathematical Olympiad jury grader.
            Task: Evaluate whether the student's candidate answer is mathematically and semantically EQUIVALENT to the official answer key.
            
            Olympiad Problem Statement:
            ${question.latexContent}
            
            Official Canonical Answer:
            $trimmedCorrect
            
            Student's Candidate Answer:
            $trimmedCandidate
            
            Instructions:
            - Focus strictly on semantic and mathematical correctness.
            - Accept phrasing synonyms (e.g. "not possible", "impossible", "no solutions exist", "none").
            - Accept equivalent algebraic expressions or unsimplified forms if mathematically identical.
            - Output strictly ONE WORD: "CORRECT" or "INCORRECT".
        """.trimIndent()

        try {
            val request = ChatCompletionRequest(
                messages = listOf(
                    ChatMessage(
                        role = "system",
                        content = "You are an expert Math Olympiad answer grader. Reply strictly with CORRECT or INCORRECT."
                    ),
                    ChatMessage(
                        role = "user",
                        content = evaluationPrompt
                    )
                )
            )

            val response = apiService.getChatCompletion(request)
            if (response.isSuccessful) {
                val verdict = response.body()?.choices?.firstOrNull()?.message?.content?.trim()?.uppercase()
                if (verdict != null) {
                    val isCorrect = verdict.contains("CORRECT") && !verdict.contains("INCORRECT")
                    return@withContext Result.success(isCorrect)
                }
            }

            Result.success(fallbackSemanticCheck(trimmedCorrect, trimmedCandidate))
        } catch (e: Exception) {
            Result.success(fallbackSemanticCheck(trimmedCorrect, trimmedCandidate))
        }
    }

    private fun fallbackSemanticCheck(correct: String, candidate: String): Boolean {
        val normCorrect = correct.lowercase().replace("[^a-z0-9]".toRegex(), "")
        val normCandidate = candidate.lowercase().replace("[^a-z0-9]".toRegex(), "")
        if (normCorrect == normCandidate) return true

        val impossibilityPhrases = setOf("impossible", "notpossible", "nosolution", "nosolutions", "none", "empty")
        if (normCorrect in impossibilityPhrases && normCandidate in impossibilityPhrases) {
            return true
        }
        return false
    }

    /**
     * Domain fallback conceptual stepping stone when external API is unreachable.
     * Guarantees that the student is never blocked even without network connection.
     */
    private fun generateOfflineSteppingStone(
        question: PracticeQuestion,
        incorrectAttempts: List<String>
    ): String {
        return when (question.category) {
            "Combinatorics" -> """Try evaluating the degree parity of vertex subset ${'$'}A modulo 2 to see whether an odd Eulerian cycle must be traversed."""
            "Algebra" -> """Notice that expanding (x - y)^3 directly combines the cubic and bilinear terms of your system into a single integer."""
            "Number Theory" -> """Examine the equation modulo the smallest prime divisor and consider the parity of the factors (p - 1)(p + 1)."""
            "Geometry" -> """Consider reflecting the orthocenter ${'$'}H across the sides of \triangle ABC onto the circumcircle."""
            else -> """Consider constructing an invariant under the problem's symmetry before substituting specific numerical candidates."""
        }
    }
}
