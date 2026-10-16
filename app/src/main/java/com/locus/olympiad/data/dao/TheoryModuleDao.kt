package com.locus.olympiad.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.locus.olympiad.data.model.TheoryModule
import kotlinx.coroutines.flow.Flow

@Dao
interface TheoryModuleDao {

    @Query("SELECT * FROM theory_modules WHERE id = :moduleId")
    fun getModuleById(moduleId: Long): Flow<TheoryModule?>

    @Query("SELECT * FROM theory_modules WHERE id = :moduleId")
    suspend fun getModuleByIdSync(moduleId: Long): TheoryModule?

    @Query("SELECT * FROM theory_modules WHERE category = :category ORDER BY chapter_number ASC, id ASC")
    fun getModulesByCategory(category: String): Flow<List<TheoryModule>>

    @Query("SELECT * FROM theory_modules WHERE category = :category ORDER BY chapter_number ASC, id ASC")
    suspend fun getModulesByCategorySync(category: String): List<TheoryModule>

    @Query("SELECT * FROM theory_modules ORDER BY category ASC, chapter_number ASC")
    fun getAllModules(): Flow<List<TheoryModule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: TheoryModule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<TheoryModule>): List<Long>

    @Update
    suspend fun updateModule(module: TheoryModule)

    @Query("UPDATE theory_modules SET read_progress_percent = :progress WHERE id = :moduleId")
    suspend fun updateProgress(moduleId: Long, progress: Int)

    @Query("SELECT COUNT(*) FROM theory_modules")
    suspend fun countModules(): Int
}
