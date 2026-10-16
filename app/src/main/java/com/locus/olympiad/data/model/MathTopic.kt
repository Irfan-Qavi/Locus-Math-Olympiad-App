package com.locus.olympiad.data.model

/**
 * The four pillars of International Mathematical Olympiad (IMO), IOQM, and RMO.
 */
enum class MathTopic(val displayName: String) {
    ALGEBRA("Algebra"),
    GEOMETRY("Geometry"),
    COMBINATORICS("Combinatorics"),
    NUMBER_THEORY("Number Theory");

    companion object {
        fun fromString(value: String): MathTopic {
            return entries.firstOrNull { 
                it.displayName.equals(value, ignoreCase = true) || 
                it.name.equals(value, ignoreCase = true) 
            } ?: ALGEBRA
        }

        val ALL_TOPICS = listOf(ALGEBRA, GEOMETRY, COMBINATORICS, NUMBER_THEORY)
    }
}
