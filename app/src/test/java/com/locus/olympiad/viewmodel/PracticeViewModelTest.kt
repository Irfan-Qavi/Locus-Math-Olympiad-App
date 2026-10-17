package com.locus.olympiad.viewmodel

import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.domain.elo.EloCalculationResult
import com.locus.olympiad.repository.MatchmakingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val matchmakingRepository = mockk<MatchmakingRepository>()
    private val aiTutorRepository = mockk<AiTutorRepository>()

    private val testQuestion = PracticeQuestion(
        id = 1,
        category = "Combinatorics",
        difficultyElo = 1000,
        latexContent = "Calculate $\\chi(G)$",
        correctAnswer = "3",
        listOfHints = listOf("Hint 1", "Hint 2", "Hint 3")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testRevealNextHint_IncrementsCountAndExposesHints() {
        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository)
        // Inject question into state
        viewModel.updateDraftAnswer("test")

        // Initial count is 0
        assertEquals(0, viewModel.uiState.value.revealedHintsCount)

        // Reveal hint 1
        // We can simulate having a loaded question
        coEvery { matchmakingRepository.getNextPracticeQuestion(any()) } returns testQuestion
        viewModel.loadNextQuestion(testQuestion.topic)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.revealedHintsCount)
        assertEquals(0, viewModel.uiState.value.visibleHints.size)

        viewModel.revealNextHint()
        assertEquals(1, viewModel.uiState.value.revealedHintsCount)
        assertEquals(listOf("Hint 1"), viewModel.uiState.value.visibleHints)

        viewModel.revealNextHint()
        assertEquals(2, viewModel.uiState.value.revealedHintsCount)
        assertEquals(listOf("Hint 1", "Hint 2"), viewModel.uiState.value.visibleHints)
    }

    @Test
    fun testHardStuckTrigger_FiresAiTutorAfter3IncorrectAttempts() = runTest {
        coEvery { matchmakingRepository.getNextPracticeQuestion(any()) } returns testQuestion
        coEvery { matchmakingRepository.submitAnswer(testQuestion, any()) } returns Pair(
            false,
            EloCalculationResult(1000, 984, -16, 0.5, false, 1000)
        )
        coEvery { aiTutorRepository.getConceptualHint(any(), any(), any()) } returns Result.success(
            "Consider the parity of the degree sum over \$A."
        )

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository)
        viewModel.loadNextQuestion(testQuestion.topic)
        advanceUntilIdle()

        // Attempt 1: incorrect
        viewModel.submitAnswer("1")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isHardStuck)
        assertEquals(listOf("1"), viewModel.uiState.value.incorrectAttempts)

        // Attempt 2: incorrect
        viewModel.submitAnswer("2")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isHardStuck)
        assertEquals(listOf("1", "2"), viewModel.uiState.value.incorrectAttempts)

        // Attempt 3: incorrect -> triggers hard stuck and AI tutor call
        viewModel.submitAnswer("4")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isHardStuck)
        assertEquals(listOf("1", "2", "4"), viewModel.uiState.value.incorrectAttempts)
        assertEquals("Consider the parity of the degree sum over \$A.", viewModel.uiState.value.aiTutorHint)

        // Verify AI Tutor repository was called
        coVerify(exactly = 1) {
            aiTutorRepository.getConceptualHint(testQuestion, 984, listOf("1", "2", "4"))
        }
    }
}
