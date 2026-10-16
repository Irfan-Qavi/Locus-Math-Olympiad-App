package com.locus.olympiad.data

import com.locus.olympiad.data.model.MathTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SeedDataParserTest {

    @Test
    fun testParseSeedQuestions_ParsesValidJsonString() {
        val sampleJson = """
            [
              {
                "category": "Combinatorics",
                "difficulty_elo": 1080,
                "latex_content": "Find the number of permutations $\\pi$.",
                "correct_answer": "90",
                "list_of_hints": ["Hint 1", "Hint 2"],
                "source_contest": "IOQM Final Stage",
                "tier": "Tier II"
              },
              {
                "category": "Algebra",
                "difficulty_elo": 1020,
                "latex_content": "Find all real solutions $(x, y)$.",
                "correct_answer": "3",
                "list_of_hints": ["Hint A"],
                "source_contest": "RMO Algebra Round",
                "tier": "Tier II"
              }
            ]
        """.trimIndent()

        val parsed = SeedDataParser.parseSeedQuestionsFromJson(sampleJson)

        assertEquals(2, parsed.size)

        val q1 = parsed[0]
        assertEquals("Combinatorics", q1.category)
        assertEquals(MathTopic.COMBINATORICS, q1.topic)
        assertEquals(1080, q1.difficultyElo)
        assertEquals("Find the number of permutations $\\pi$.", q1.latexContent)
        assertEquals("90", q1.correctAnswer)
        assertEquals(2, q1.listOfHints.size)
        assertEquals("IOQM Final Stage", q1.sourceContest)
        assertEquals("Tier II", q1.tier)

        val q2 = parsed[1]
        assertEquals("Algebra", q2.category)
        assertEquals(MathTopic.ALGEBRA, q2.topic)
        assertEquals(1020, q2.difficultyElo)
        assertEquals("3", q2.correctAnswer)
    }

    @Test
    fun testActualAssetSeedQuestionsFile_IsValidJsonAndContainsAllFourTopics() {
        // Read actual asset file from project repository
        val assetFile = File("src/main/assets/questions_seed.json")
        val altAssetFile = File("app/src/main/assets/questions_seed.json")
        val targetFile = if (assetFile.exists()) assetFile else altAssetFile

        assertTrue("questions_seed.json asset file must exist", targetFile.exists())

        val jsonContent = targetFile.readText(Charsets.UTF_8)
        val questions = SeedDataParser.parseSeedQuestionsFromJson(jsonContent)

        assertTrue("Seed questions must contain multiple entries", questions.size >= 12)

        val categories = questions.map { it.category }.toSet()
        assertTrue("Must contain Combinatorics", categories.contains("Combinatorics"))
        assertTrue("Must contain Algebra", categories.contains("Algebra"))
        assertTrue("Must contain Number Theory", categories.contains("Number Theory"))
        assertTrue("Must contain Geometry", categories.contains("Geometry"))

        questions.forEach { question ->
            assertNotNull(question.category)
            assertTrue("difficulty_elo must be positive", question.difficultyElo > 0)
            assertTrue("latex_content must not be blank", question.latexContent.isNotBlank())
            assertTrue("correct_answer must not be blank", question.correctAnswer.isNotBlank())
            assertTrue("list_of_hints must not be empty", question.listOfHints.isNotEmpty())
        }
    }
}
