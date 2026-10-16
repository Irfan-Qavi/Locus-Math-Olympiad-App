package com.locus.olympiad.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Practice question for IOQM/RMO/IMO math olympiad preparation.
 * Contains LaTeX problem statement, Elo difficulty rating, correct answer, and progressive hints.
 */
@Entity(tableName = "practice_questions")
data class PracticeQuestion(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Long = 0,

    @ColumnInfo(name = "category")
    @SerializedName("category")
    val category: String, // Algebra, Geometry, Combinatorics, Number Theory

    @ColumnInfo(name = "difficulty_elo")
    @SerializedName("difficulty_elo", alternate = ["difficultyElo"])
    val difficultyElo: Int,

    @ColumnInfo(name = "latex_content")
    @SerializedName("latex_content", alternate = ["latexContent"])
    val latexContent: String,

    @ColumnInfo(name = "correct_answer")
    @SerializedName("correct_answer", alternate = ["correctAnswer"])
    val correctAnswer: String,

    @ColumnInfo(name = "list_of_hints")
    @SerializedName("list_of_hints", alternate = ["listOfHints"])
    val listOfHints: List<String> = emptyList(),

    @ColumnInfo(name = "source_contest")
    @SerializedName("source_contest", alternate = ["sourceContest"])
    val sourceContest: String? = "IOQM / RMO Practice",

    @ColumnInfo(name = "tier")
    @SerializedName("tier")
    val tier: String = "Tier III"
) {
    val topic: MathTopic
        get() = MathTopic.fromString(category)
}
