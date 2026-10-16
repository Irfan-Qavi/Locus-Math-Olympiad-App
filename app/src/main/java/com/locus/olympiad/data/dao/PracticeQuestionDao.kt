package com.locus.olympiad.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.locus.olympiad.data.model.PracticeQuestion
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeQuestionDao {

    @Query("SELECT * FROM practice_questions WHERE id = :questionId")
    fun getQuestionById(questionId: Long): Flow<PracticeQuestion?>

    @Query("SELECT * FROM practice_questions WHERE id = :questionId")
    suspend fun getQuestionByIdSync(questionId: Long): PracticeQuestion?

    @Query("SELECT * FROM practice_questions WHERE category = :category ORDER BY id ASC")
    fun getQuestionsByCategory(category: String): Flow<List<PracticeQuestion>>

    @Query("SELECT * FROM practice_questions WHERE category = :category ORDER BY id ASC")
    suspend fun getQuestionsByCategorySync(category: String): List<PracticeQuestion>

    /**
     * Primary Matchmaking Query:
     * Returns a question in the selected category where difficulty_elo is within [minElo, maxElo] (+/- 150 pts).
     */
    @Query("""
        SELECT * FROM practice_questions 
        WHERE category = :category 
          AND difficulty_elo BETWEEN :minElo AND :maxElo 
        ORDER BY RANDOM() 
        LIMIT 1
    """)
    suspend fun findQuestionInEloRange(
        category: String, 
        minElo: Int, 
        maxElo: Int
    ): PracticeQuestion?

    /**
     * Fallback Matchmaking Query:
     * When no question exists strictly within +/- 150 points, return the closest available question by Elo difference.
     */
    @Query("""
        SELECT * FROM practice_questions 
        WHERE category = :category 
        ORDER BY ABS(difficulty_elo - :targetElo) ASC 
        LIMIT 1
    """)
    suspend fun findClosestQuestion(
        category: String, 
        targetElo: Int
    ): PracticeQuestion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: PracticeQuestion): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<PracticeQuestion>): List<Long>

    @Query("SELECT COUNT(*) FROM practice_questions")
    suspend fun countQuestions(): Int

    @Query("SELECT COUNT(*) FROM practice_questions WHERE category = :category")
    suspend fun countQuestionsByCategory(category: String): Int
}
