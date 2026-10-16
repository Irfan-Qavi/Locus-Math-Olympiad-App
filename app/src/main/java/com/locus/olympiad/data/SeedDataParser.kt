package com.locus.olympiad.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.locus.olympiad.data.model.PracticeQuestion
import java.io.InputStreamReader

/**
 * Utility to parse pre-populated math olympiad seed questions from the local assets folder.
 */
object SeedDataParser {

    private const val SEED_QUESTIONS_ASSET_FILE = "questions_seed.json"

    /**
     * Reads and parses seed questions from the application assets directory.
     */
    fun parseSeedQuestions(context: Context, fileName: String = SEED_QUESTIONS_ASSET_FILE): List<PracticeQuestion> {
        return try {
            context.assets.open(fileName).use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    val listType = object : TypeToken<List<PracticeQuestion>>() {}.type
                    Gson().fromJson<List<PracticeQuestion>>(reader, listType) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Helper to parse questions directly from a raw JSON string (convenient for unit testing).
     */
    fun parseSeedQuestionsFromJson(jsonString: String): List<PracticeQuestion> {
        return try {
            val listType = object : TypeToken<List<PracticeQuestion>>() {}.type
            Gson().fromJson<List<PracticeQuestion>>(jsonString, listType) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
