package com.locus.olympiad.repository

import com.google.code.gson.JsonParser
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

        // 2. Build prompt for LLM semantic evaluation with strict JSON schema
        val systemPrompt = "You are evaluating high school Olympiad mathematics. You must mark the student's answer as CORRECT if it is mathematically and semantically equivalent to the expected answer, regardless of phrasing. For example, 'Such a configuration does not exist', '0 solutions', 'None', and 'Impossible' are all strictly equivalent. Return strictly a JSON object: {\"isCorrect\": true} or {\"isCorrect\": false}."

        val evaluationPrompt = """
            Evaluate whether the student's candidate answer is mathematically and semantically equivalent to the official expected answer.

            Olympiad Problem Statement:
            ${question.latexContent}

            Official Expected Answer:
            $trimmedCorrect

            Student's Candidate Answer:
            $trimmedCandidate

            Return strictly a JSON object:
            {"isCorrect": true} or {"isCorrect": false}
        """.trimIndent()

        try {
            val request = ChatCompletionRequest(
                messages = listOf(
                    ChatMessage(
                        role = "system",
                        content = systemPrompt
                    ),
                    ChatMessage(
                        role = "user",
                        content = evaluationPrompt
                    )
                )
            )

            val response = apiService.getChatCompletion(request)
            if (response.isSuccessful) {
                val rawBody = response.body()?.choices?.firstOrNull()?.message?.content?.trim()
                if (!rawBody.isNullOrBlank()) {
                    val jsonCandidate = rawBody.substringAfter("{", "").let {
                        if (it.isNotEmpty()) "{" + it.substringBeforeLast("}", "") + "}" else rawBody
                    }
                    try {
                        val jsonObject = JsonParser.parseString(jsonCandidate).asJsonObject
                        if (jsonObject.has("isCorrect")) {
                            return@withContext Result.success(jsonObject.get("isCorrect").asBoolean)
                        }
                    } catch (_: Exception) {
                        if (rawBody.contains("\"isCorrect\"\\s*:\\s*true".toRegex(RegexOption.IGNORE_CASE))) {
                            return@withContext Result.success(true)
                        } else if (rawBody.contains("\"isCorrect\"\\s*:\\s*false".toRegex(RegexOption.IGNORE_CASE))) {
                            return@withContext Result.success(false)
                        }
                    }
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

        val impossibilityKeywords = listOf(
            "impossible", "notpossible", "nosolution", "nosolutions", "none", "empty",
            "doesnotexist", "suchaconfigurationdoesnotexist", "notexist", "nonexistent", "0solutions"
        )
        val correctIsImpossible = impossibilityKeywords.any { normCorrect.contains(it) || it.contains(normCorrect) }
        val candidateIsImpossible = impossibilityKeywords.any { normCandidate.contains(it) || it.contains(normCandidate) }
        if (correctIsImpossible && candidateIsImpossible) {
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
