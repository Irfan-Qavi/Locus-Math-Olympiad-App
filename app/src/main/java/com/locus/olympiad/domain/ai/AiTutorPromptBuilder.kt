package com.locus.olympiad.domain.ai

/**
 * Builds the prompt template for the AI Olympiad Tutor.
 * Enforces strict pedagogical guardrails preventing answer leakage.
 */
object AiTutorPromptBuilder {

    /**
     * Builds the exact Olympiad coaching prompt for a hard-stuck student.
     */
    fun buildPrompt(
        userElo: Int,
        category: String,
        latexContent: String,
        incorrectAttempts: List<String>
    ): String {
        // Take the 3 most recent attempts
        val recentAttempts = if (incorrectAttempts.size >= 3) {
            incorrectAttempts.takeLast(3)
        } else {
            incorrectAttempts
        }

        val attempt1 = recentAttempts.getOrNull(0) ?: "None"
        val attempt2 = recentAttempts.getOrNull(1) ?: "None"
        val attempt3 = recentAttempts.getOrNull(2) ?: "None"

        return """You are Euler, an elite International Math Olympiad coach.
The student has a current Elo rating of $userElo in $category.

Problem Statement: 
"$latexContent"

The student is hard-stuck and has submitted the following incorrect answers:
1. "$attempt1"
2. "$attempt2"
3. "$attempt3"

YOUR TASK:
Provide exactly ONE concise, conceptual hint sentence. 
- You MUST diagnose their misconception based on their incorrect attempts.
- You MUST suggest a mathematical stepping stone (an invariant, a monovariant, a specific substitution, or a geometric construction).
- You MUST NOT reveal the final numerical answer, the final algebraic expression, or perform the final calculation step for them.
- Format any math in the hint using standard LaTeX delimiters."""
    }
}
