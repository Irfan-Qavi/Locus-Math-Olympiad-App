package com.locus.olympiad.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.locus.olympiad.data.converter.RoomConverters
import com.locus.olympiad.data.dao.PracticeQuestionDao
import com.locus.olympiad.data.dao.TheoryModuleDao
import com.locus.olympiad.data.dao.UserTopicProfileDao
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.TheoryModule
import com.locus.olympiad.data.model.UserTopicProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserTopicProfile::class,
        PracticeQuestion::class,
        TheoryModule::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class LocusDatabase : RoomDatabase() {

    abstract fun userTopicProfileDao(): UserTopicProfileDao
    abstract fun practiceQuestionDao(): PracticeQuestionDao
    abstract fun theoryModuleDao(): TheoryModuleDao

    companion object {
        @Volatile
        private var INSTANCE: LocusDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): LocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocusDatabase::class.java,
                    "locus_olympiad.db"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch {
                            getInstance(context, scope).let { database ->
                                DatabaseInitializer.seedDatabase(database)
                            }
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
