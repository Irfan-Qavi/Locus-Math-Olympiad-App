package com.locus.olympiad.viewmodel

import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.PracticeSessionConfig
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.domain.elo.EloCalculationResult
import com.locus.olympiad.repository.MatchmakingRepository
import io.mockk.coEvery
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
        correctAnswer = "3"
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
    fun testStartSession_InitializesSessionStateAndLoadsQuestion() = runTest {
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any()) } returns testQuestion

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository)
        val config = PracticeSessionConfig(questionCount = 5, difficultyTier = "Intermediate")
        viewModel.startSession(config)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sessionQuestionIndex)
        assertEquals(0, viewModel.uiState.value.sessionSolvedCount)
        assertEquals(testQuestion, viewModel.uiState.value.currentQuestion)
    }

    @Test
    fun testSubmitAnswer_CorrectAnswerUpdatesEloAndSolvedCount() = runTest {
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any()) } returns testQuestion
        coEvery { matchmakingRepository.submitAnswer(testQuestion, "3", any()) } returns Pair(
            true,
            EloCalculationResult(1000, 1016, 16, 0.5, true, 1000)
        )

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository)
        viewModel.loadNextQuestion(testQuestion.topic)
        advanceUntilIdle()

        viewModel.submitAnswer("3")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastSubmissionCorrect == true)
        assertEquals(1016, viewModel.uiState.value.userElo)
        assertEquals(1, viewModel.uiState.value.sessionSolvedCount)
        assertEquals(16, viewModel.uiState.value.sessionNetEloDelta)
    }

    @Test
    fun testSubmitProofSelfEvaluation_UpdatesRatingAndSessionStatus() = runTest {
        val proofQuestion = PracticeQuestion(
            id = 2,
            category = "Algebra",
            difficultyElo = 1450,
            questionType = "SUBJECTIVE_PROOF",
            latexContent = "Prove Nesbitt Inequality",
            correctAnswer = "Nesbitt Inequality holds",
            proofSolution = "By Cauchy-Schwarz in Engel form..."
        )
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any()) } returns proofQuestion
        coEvery { matchmakingRepository.submitAnswer(proofQuestion, "PROVED", any()) } returns Pair(
            true,
            EloCalculationResult(1000, 1000, 0, 0.5, true, 1450)
        )

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository)
        viewModel.loadNextQuestion(proofQuestion.topic)
        advanceUntilIdle()

        viewModel.submitProofSelfEvaluation(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastSubmissionCorrect == true)
        assertEquals(1, viewModel.uiState.value.sessionSolvedCount)
        assertEquals(0, viewModel.uiState.value.sessionNetEloDelta)
    }
}
