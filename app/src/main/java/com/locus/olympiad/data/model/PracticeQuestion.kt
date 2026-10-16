package com.locus.olympiad.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Practice question for IOQM/RMO/IMO math olympiad preparation.
 * Contains LaTeX problem statement, Elo difficulty rating, correct answer, and progressive hints.
 */
@Entity(tableName = "practice_questions")
data class PracticeQuestion(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "category")
    val category: String, // Algebra, Geometry, Combinatorics, Number Theory

    @ColumnInfo(name = "difficulty_elo")
    val difficultyElo: Int,

    @ColumnInfo(name = "latex_content")
    val latexContent: String,

    @ColumnInfo(name = "correct_answer")
    val correctAnswer: String,

    @ColumnInfo(name = "list_of_hints")
    val listOfHints: List<String> = emptyList(),

    @ColumnInfo(name = "source_contest")
    val sourceContest: String? = "IOQM / RMO Practice",

    @ColumnInfo(name = "tier")
    val tier: String = "Tier III"
) {
    val topic: MathTopic
        get() = MathTopic.fromString(category)
}
