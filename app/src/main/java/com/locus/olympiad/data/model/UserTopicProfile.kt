package com.locus.olympiad.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks the user's competitive Elo rating in each of the four mathematical olympiad domains.
 * Default starting Elo is 1000.
 */
@Entity(tableName = "user_topic_profiles")
data class UserTopicProfile(
    @PrimaryKey
    @ColumnInfo(name = "category")
    val category: String, // e.g. "Algebra", "Geometry", "Combinatorics", "Number Theory"

    @ColumnInfo(name = "elo_rating")
    val eloRating: Int = DEFAULT_ELO,

    @ColumnInfo(name = "questions_solved")
    val questionsSolved: Int = 0,

    @ColumnInfo(name = "questions_attempted")
    val questionsAttempted: Int = 0,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val topic: MathTopic
        get() = MathTopic.fromString(category)

    companion object {
        const val DEFAULT_ELO = 1000

        fun createInitialProfiles(): List<UserTopicProfile> {
            return MathTopic.ALL_TOPICS.map { topic ->
                UserTopicProfile(
                    category = topic.displayName,
                    eloRating = DEFAULT_ELO
                )
            }
        }
    }
}
