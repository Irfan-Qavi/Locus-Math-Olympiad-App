package com.locus.olympiad.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.locus.olympiad.data.LocusDatabase
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.TheoryModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker responsible for syncing remote Olympiad content
 * (thousands of practice questions and theory modules) from Cloud Firestore into Room.
 * The UI continues reading strictly from Room for zero latency, while Room is
 * asynchronously populated by this worker.
 */
class RemoteContentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting RemoteContentSyncWorker execution...")

        val database = LocusDatabase.getInstance(applicationContext)
        val questionDao = database.practiceQuestionDao()
        val theoryDao = database.theoryModuleDao()

        try {
            val firestore = FirebaseFirestore.getInstance()

            // 1. Sync Practice Questions in batches of 100
            val syncedQuestions = syncPracticeQuestions(firestore, questionDao)

            // 2. Sync Theory Modules in batches of 100
            val syncedModules = syncTheoryModules(firestore, theoryDao)

            Log.i(TAG, "Sync complete. New questions inserted: $syncedQuestions, new theory modules inserted: $syncedModules")
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Remote content sync warning/error: ${e.localizedMessage}", e)
            // If offline or service unreachable, allow retry later
            Result.retry()
        }
    }

    private suspend fun syncPracticeQuestions(
        firestore: FirebaseFirestore,
        questionDao: com.locus.olympiad.data.dao.PracticeQuestionDao
    ): Int {
        var insertedCount = 0
        try {
            val querySnapshot: QuerySnapshot = firestore.collection(COLLECTION_QUESTIONS)
                .limit(BATCH_SIZE)
                .get()
                .await()

            val candidateQuestions = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getLong("id") ?: doc.id.hashCode().toLong().coerceAtLeast(1L)
                    val category = doc.getString("category") ?: "Algebra"
                    val difficultyElo = doc.getLong("difficulty_elo")?.toInt()
                        ?: doc.getLong("difficultyElo")?.toInt() ?: 1200
                    val latexContent = doc.getString("latex_content")
                        ?: doc.getString("latexContent") ?: ""
                    val correctAnswer = doc.getString("correct_answer")
                        ?: doc.getString("correctAnswer") ?: ""
                    val sourceContest = doc.getString("source_contest")
                        ?: doc.getString("sourceContest")
                    val tier = doc.getString("tier") ?: "Tier III"
                    val questionType = doc.getString("question_type")
                        ?: doc.getString("questionType") ?: "OBJECTIVE"
                    val proofSolution = doc.getString("proof_solution")
                        ?: doc.getString("proofSolution")
                    val source = doc.getString("source")

                    PracticeQuestion(
                        id = id,
                        category = category,
                        difficultyElo = difficultyElo,
                        latexContent = latexContent,
                        correctAnswer = correctAnswer,
                        sourceContest = sourceContest,
                        tier = tier,
                        questionType = questionType,
                        proofSolution = proofSolution,
                        source = source
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse question doc: ${doc.id}", e)
                    null
                }
            }

            // Filter for items not currently present in local Room database
            val newQuestions = candidateQuestions.filter { question ->
                questionDao.getQuestionByIdSync(question.id) == null
            }

            if (newQuestions.isNotEmpty()) {
                questionDao.insertQuestions(newQuestions)
                insertedCount = newQuestions.size
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying Firestore questions collection: ${e.localizedMessage}")
        }
        return insertedCount
    }

    private suspend fun syncTheoryModules(
        firestore: FirebaseFirestore,
        theoryDao: com.locus.olympiad.data.dao.TheoryModuleDao
    ): Int {
        var insertedCount = 0
        try {
            val querySnapshot: QuerySnapshot = firestore.collection(COLLECTION_THEORY)
                .limit(BATCH_SIZE)
                .get()
                .await()

            val candidateModules = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.getLong("id") ?: doc.id.hashCode().toLong().coerceAtLeast(1L)
                    val category = doc.getString("category") ?: "Algebra"
                    val chapterName = doc.getString("chapter_name")
                        ?: doc.getString("chapterName") ?: "Chapter 1: Foundational Lemmas"
                    val chapterNumber = doc.getLong("chapter_number")?.toInt()
                        ?: doc.getLong("chapterNumber")?.toInt() ?: 1
                    val title = doc.getString("title") ?: ""
                    val latexContent = doc.getString("latex_content")
                        ?: doc.getString("latexContent") ?: ""
                    val heuristics = doc.getString("heuristics_and_tricks")
                        ?: doc.getString("heuristicsAndTricks") ?: doc.getString("tips")
                    val progress = doc.getLong("read_progress_percent")?.toInt()
                        ?: doc.getLong("readProgressPercent")?.toInt() ?: 0

                    TheoryModule(
                        id = id,
                        category = category,
                        chapterName = chapterName,
                        chapterNumber = chapterNumber,
                        title = title,
                        latexContent = latexContent,
                        heuristicsAndTricks = heuristics,
                        readProgressPercent = progress
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse theory doc: ${doc.id}", e)
                    null
                }
            }

            // Filter for modules not currently present in local Room database
            val newModules = candidateModules.filter { module ->
                theoryDao.getModuleByIdSync(module.id) == null
            }

            if (newModules.isNotEmpty()) {
                theoryDao.insertModules(newModules)
                insertedCount = newModules.size
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying Firestore theory collection: ${e.localizedMessage}")
        }
        return insertedCount
    }

    companion object {
        const val TAG = "RemoteContentSyncWorker"
        const val WORK_NAME = "locus_remote_content_sync"
        const val COLLECTION_QUESTIONS = "practice_questions"
        const val COLLECTION_THEORY = "theory_modules"
        private const val BATCH_SIZE = 100L

        /**
         * Enqueues periodic background synchronization scheduled every 12 hours
         * with connected network constraint.
         */
        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<RemoteContentSyncWorker>(
                repeatInterval = 12,
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        /**
         * Enqueues a one-time immediate background synchronization.
         */
        fun enqueueImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<RemoteContentSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }
}
