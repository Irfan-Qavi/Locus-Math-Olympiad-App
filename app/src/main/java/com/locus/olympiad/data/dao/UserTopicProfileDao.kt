package com.locus.olympiad.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.locus.olympiad.data.model.UserTopicProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTopicProfileDao {

    @Query("SELECT * FROM user_topic_profiles WHERE category = :category")
    fun getProfileByCategory(category: String): Flow<UserTopicProfile?>

    @Query("SELECT * FROM user_topic_profiles WHERE category = :category")
    suspend fun getProfileByCategorySync(category: String): UserTopicProfile?

    @Query("SELECT elo_rating FROM user_topic_profiles WHERE category = :category")
    suspend fun getEloRatingForCategory(category: String): Int?

    @Query("SELECT * FROM user_topic_profiles ORDER BY category ASC")
    fun getAllProfiles(): Flow<List<UserTopicProfile>>

    @Query("SELECT * FROM user_topic_profiles ORDER BY category ASC")
    suspend fun getAllProfilesSync(): List<UserTopicProfile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserTopicProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<UserTopicProfile>)

    @Update
    suspend fun updateProfile(profile: UserTopicProfile)

    @Query("UPDATE user_topic_profiles SET elo_rating = :newElo, last_updated = :updatedAt WHERE category = :category")
    suspend fun updateEloRating(
        category: String, 
        newElo: Int, 
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE user_topic_profiles 
        SET elo_rating = :newElo, 
            questions_solved = questions_solved + :solvedIncrement, 
            questions_attempted = questions_attempted + 1,
            last_updated = :updatedAt 
        WHERE category = :category
    """)
    suspend fun recordSubmissionResult(
        category: String,
        newElo: Int,
        solvedIncrement: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("SELECT COUNT(*) FROM user_topic_profiles")
    suspend fun countProfiles(): Int
}
