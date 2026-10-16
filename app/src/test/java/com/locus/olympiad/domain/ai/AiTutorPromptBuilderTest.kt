package com.locus.olympiad.domain.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class AiTutorPromptBuilderTest {

    @Test
    fun testBuildPrompt_ContainsRequiredOlympiadCoachTemplateAndAttempts() {
        val userElo = 1140
        val category = "Combinatorics"
        val latex = "Determine \\chi(G)"
        val attempts = listOf("1", "2", "4")

        val prompt = AiTutorPromptBuilder.buildPrompt(
            userElo = userElo,
            category = category,
            latexContent = latex,
            incorrectAttempts = attempts
        )

        // Check required template elements
        assertTrue(prompt.contains("You are Euler, an elite International Math Olympiad coach."))
        assertTrue(prompt.contains("The student has a current Elo rating of 1140 in Combinatorics."))
        assertTrue(prompt.contains("Determine \\chi(G)"))
        assertTrue(prompt.contains("1. \"1\""))
        assertTrue(prompt.contains("2. \"2\""))
        assertTrue(prompt.contains("3. \"4\""))
        assertTrue(prompt.contains("Provide exactly ONE concise, conceptual hint sentence."))
        assertTrue(prompt.contains("You MUST NOT reveal the final numerical answer"))
    }
}
