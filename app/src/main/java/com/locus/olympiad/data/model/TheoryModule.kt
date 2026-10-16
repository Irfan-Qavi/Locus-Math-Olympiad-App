package com.locus.olympiad.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Theoretical module or monograph covering foundational lemmas, theorems, and techniques
 * for Olympiad preparation (e.g. Alon's Nullstellensatz, Dirichlet Approximation, etc.).
 */
@Entity(tableName = "theory_modules")
data class TheoryModule(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "category")
    val category: String, // Algebra, Geometry, Combinatorics, Number Theory

    @ColumnInfo(name = "latex_content")
    val latexContent: String,

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "chapter_number")
    val chapterNumber: Int = 1,

    @ColumnInfo(name = "read_progress_percent")
    val readProgressPercent: Int = 0
) {
    val topic: MathTopic
        get() = MathTopic.fromString(category)
}
