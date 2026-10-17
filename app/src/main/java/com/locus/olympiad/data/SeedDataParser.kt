package com.locus.olympiad.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.locus.olympiad.data.model.PracticeQuestion
import java.io.InputStreamReader

/**
 * Utility to parse pre-populated math olympiad seed questions from the local assets folder.
 * Supports memory-safe streaming via Gson's JsonReader to process 4,000+ questions in
 * chunked transactions without causing Out-Of-Memory (OOM) or Application Not Responding (ANR).
 */
object SeedDataParser {

    private const val SEED_QUESTIONS_ASSET_FILE = "questions_seed.json"
    private const val DEFAULT_CHUNK_SIZE = 100

    /**
     * Memory-safe streaming parser:
     * Sequentially reads the JSON array element by element using [JsonReader] and invokes
     * [onBatchReady] every [chunkSize] items (default 100).
     *
     * Keeps memory footprint constant at O(1) regardless of whether the dataset contains
     * 100 or 10,000 questions.
     *
     * @return Total number of questions ingested.
     */
    fun streamSeedQuestionsInChunks(
        context: Context,
        fileName: String = SEED_QUESTIONS_ASSET_FILE,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        onBatchReady: suspend (List<PracticeQuestion>) -> Unit
    ): Int {
        var totalIngested = 0
        try {
            val gson = Gson()
            context.assets.open(fileName).use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { isr ->
                    JsonReader(isr).use { jsonReader ->
                        jsonReader.beginArray()
                        val buffer = ArrayList<PracticeQuestion>(chunkSize)

                        while (jsonReader.hasNext()) {
                            val question: PracticeQuestion? = gson.fromJson(jsonReader, PracticeQuestion::class.java)
                            if (question != null) {
                                buffer.add(question)
                                totalIngested++
                                if (buffer.size >= chunkSize) {
                                    kotlinx.coroutines.runBlocking {
                                        onBatchReady(buffer)
                                    }
                                    buffer.clear()
                                }
                            }
                        }

                        if (buffer.isNotEmpty()) {
                            kotlinx.coroutines.runBlocking {
                                onBatchReady(buffer)
                            }
                            buffer.clear()
                        }
                        jsonReader.endArray()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return totalIngested
    }

    /**
     * Legacy bulk parser for small files or unit testing.
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
