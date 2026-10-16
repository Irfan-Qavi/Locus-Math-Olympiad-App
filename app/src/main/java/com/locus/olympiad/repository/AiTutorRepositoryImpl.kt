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
        // Construct the prompt using the required prompt template
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
                        content = "You are Euler, an elite International Math Olympiad coach. Always adhere strictly to the one-sentence conceptual hint rule."
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

            // Fallback stepping stone if API call was unsuccessful or unauthenticated
            Result.success(generateOfflineSteppingStone(question, incorrectAttempts))
        } catch (e: Exception) {
            // Graceful pedagogical fallback when offline
            Result.success(generateOfflineSteppingStone(question, incorrectAttempts))
        }
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
            "Combinatorics" -> "Try evaluating the degree parity of vertex subset $A$ modulo 2 to see whether an odd Eulerian cycle must be traversed."
            "Algebra" -> "Notice that expanding $(x - y)^3$ directly combines the cubic and bilinear terms of your system into a single integer."
            "Number Theory" -> "Examine the equation modulo the smallest prime divisor and consider the parity of the factors $(p - 1)(p + 1)$."
            "Geometry" -> "Consider reflecting the orthocenter $H$ across the sides of $\\triangle ABC$ onto the circumcircle."
            else -> "Consider constructing an invariant under the problem's symmetry before substituting specific numerical candidates."
        }
    }
}
