package com.locus.olympiad.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * 3-Tier Olympiad Theory Monograph hierarchy:
 * - Tier 1: Core Olympiad Unit (Combinatorics, Algebra, Number Theory, Geometry)
 * - Tier 2: Chapter (e.g. Chapter 1: Extremal Graph Theory)
 * - Tier 3: Specific Topic / Lemma (e.g. Erdős-Szekeres Theorem, Hall's Marriage Lemma)
 *
 * Includes dedicated competition heuristics, tips, and problem-solving strategic insights.
 */
@Entity(tableName = "theory_modules")
data class TheoryModule(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: Long = 0,

    @ColumnInfo(name = "category")
    @SerializedName("category")
    val category: String, // Tier 1: Core Unit

    @ColumnInfo(name = "chapter_name")
    @SerializedName("chapter_name", alternate = ["chapterName"])
    val chapterName: String = "Chapter 1: Foundational Lemmas", // Tier 2: Chapter

    @ColumnInfo(name = "chapter_number")
    @SerializedName("chapter_number", alternate = ["chapterNumber"])
    val chapterNumber: Int = 1,

    @ColumnInfo(name = "title")
    @SerializedName("title")
    val title: String = "", // Tier 3: Specific Topic / Lemma

    @ColumnInfo(name = "latex_content")
    @SerializedName("latex_content", alternate = ["latexContent"])
    val latexContent: String,

    @ColumnInfo(name = "heuristics_and_tricks")
    @SerializedName("heuristics_and_tricks", alternate = ["heuristicsAndTricks", "tips"])
    val heuristicsAndTricks: String? = null,

    @ColumnInfo(name = "read_progress_percent")
    @SerializedName("read_progress_percent", alternate = ["readProgressPercent"])
    val readProgressPercent: Int = 0
) {
    val topic: MathTopic
        get() = MathTopic.fromString(category)
}
