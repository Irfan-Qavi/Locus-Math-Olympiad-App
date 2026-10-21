package com.locus.olympiad.repository

import com.google.gson.JsonParser
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.network.AiTutorApiService
import com.locus.olympiad.network.model.ChatCompletionRequest
import com.locus.olympiad.network.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AiTutorRepositoryImpl(
    private val apiService: AiTutorApiService
) : AiTutorRepository {

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
        val systemPrompt = """
            You are an expert mathematical olympiad grading evaluator.
            Your role is to determine whether the student's candidate answer is mathematically and semantically equivalent to the expected answer, regardless of notation, phrasing, or formatting differences.
            
            Strict Directives for Mathematical & Semantic Equivalence:
            1. Numbers, Fractions, and Decimals: Fractions and decimals representing identical real values are strictly equivalent (e.g., "1/2", "0.5", "\frac{1}{2}", and "2^{-1}" are all equivalent).
            2. Equation vs Value: Stating the variable equation vs the isolated value is equivalent (e.g., "x = 2", "x=2", "k = 2", or simply "2" are all equivalent).
            3. Algebraic & LaTeX Formats: Differences in LaTeX markup, spacing, parentheses, multiplication symbols, or variable ordering must be evaluated for algebraic equivalence (e.g., "x(x+1)", "x^2+x", "x \cdot (x+1)", and "$x^2+x$" are all equivalent).
            4. Impossibility & Existence: Synonymous statements for non-existence must be treated as equivalent (e.g., "No solution", "None", "Impossible", "0 solutions", "Empty set", "Such a configuration does not exist", and "\emptyset" are all strictly equivalent).
            5. Case & Whitespace Insensitive: Ignore capitalization, surrounding whitespace, and LaTeX dollar signs ($...$ or $$...$$).
            
            Output Requirement:
            Return STRICTLY a raw JSON object with no markdown fences, no explanatory text, and no backticks:
            {"isCorrect": true} or {"isCorrect": false}
        """.trimIndent()

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
                    // Fallback regex to safely extract the JSON object even if wrapped in markdown blocks (```json ... ``` or ``` ...)
                    val jsonRegex = Regex("""\{[\s\S]*?"isCorrect"\s*:\s*(true|false)[\s\S]*?\}""", RegexOption.IGNORE_CASE)
                    val matchedJson = jsonRegex.find(rawBody)?.value

                    val jsonCandidate = matchedJson ?: rawBody.substringAfter("{", "").let {
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
}
