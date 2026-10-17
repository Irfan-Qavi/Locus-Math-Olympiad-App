package com.locus.olympiad.data.model

/**
 * Olympiad Question Type classification:
 * - OBJECTIVE: Strict integer in [0, 99]. Standard Elo rules apply.
 * - SUBJECTIVE_SHORT: Short phrase, number, or algebraic expression evaluated semantically via AI Tutor.
 * - SUBJECTIVE_PROOF: Proof-based questions ("Prove that..."). Solution reveal with self-evaluation. Neutral Elo (0 delta).
 */
enum class QuestionType {
    OBJECTIVE,
    SUBJECTIVE_SHORT,
    SUBJECTIVE_PROOF;

    companion object {
        fun fromString(value: String?): QuestionType {
            return when (value?.uppercase()?.trim()) {
                "SUBJECTIVE_PROOF", "PROOF", "PROVE_THAT" -> SUBJECTIVE_PROOF
                "SUBJECTIVE_SHORT", "SHORT", "SUBJECTIVE" -> SUBJECTIVE_SHORT
                else -> OBJECTIVE
            }
        }
    }
}
