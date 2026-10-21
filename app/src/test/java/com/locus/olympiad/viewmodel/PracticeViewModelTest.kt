package com.locus.olympiad.viewmodel

import com.locus.olympiad.data.UserPreferencesRepository
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.PracticeSessionConfig
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.domain.elo.EloCalculationResult
import com.locus.olympiad.repository.MatchmakingRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
    private val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)

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
        coEvery { matchmakingRepository.getAllUserProfiles() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testStartSession_InitializesSessionStateAndLoadsQuestion() = runTest {
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any(), any()) } returns testQuestion

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository, userPreferencesRepository)
        val config = PracticeSessionConfig(questionCount = 5, difficultyTier = "Intermediate")
        viewModel.startSession(config)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sessionQuestionIndex)
        assertEquals(0, viewModel.uiState.value.sessionSolvedCount)
        assertEquals(testQuestion, viewModel.uiState.value.currentQuestion)
    }

    @Test
    fun testSubmitAnswer_CorrectAnswerUpdatesEloAndSolvedCount() = runTest {
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any(), any()) } returns testQuestion
        coEvery { matchmakingRepository.submitAnswer(testQuestion, "3", any(), any(), any()) } returns Pair(
            true,
            EloCalculationResult(1000, 1016, 16, 0.5, true, 1000)
        )

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository, userPreferencesRepository)
        viewModel.loadNextQuestion(testQuestion.topic)
        advanceUntilIdle()

        viewModel.submitAnswer("3")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastSubmissionCorrect == true)
        assertTrue(viewModel.uiState.value.isAnswerRevealed)
        assertEquals(1016, viewModel.uiState.value.userElo)
        assertEquals(1, viewModel.uiState.value.sessionSolvedCount)
        assertEquals(16, viewModel.uiState.value.sessionNetEloDelta)
    }

    @Test
    fun testShowAnswer_AppliesPenaltyAndRevealsAnswer() = runTest {
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any(), any()) } returns testQuestion
        coEvery { matchmakingRepository.submitAnswer(testQuestion, "SHOW_ANSWER", any(), false, true) } returns Pair(
            false,
            EloCalculationResult(1000, 982, -18, 0.5, false, 1000)
        )

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository, userPreferencesRepository)
        viewModel.startSession(PracticeSessionConfig(questionCount = 5, isRiskFree = false))
        advanceUntilIdle()

        viewModel.showAnswer()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAnswerRevealed)
        assertEquals(982, viewModel.uiState.value.userElo)
        assertEquals(-18, viewModel.uiState.value.lastEloDelta)
    }

    @Test
    fun testRiskFreeMode_GuaranteesZeroEloDelta() = runTest {
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any(), any()) } returns testQuestion
        coEvery { matchmakingRepository.submitAnswer(testQuestion, "3", any(), true, false) } returns Pair(
            true,
            EloCalculationResult(1000, 1000, 0, 0.5, true, 1000)
        )

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository, userPreferencesRepository)
        viewModel.startSession(PracticeSessionConfig(questionCount = 5, isRiskFree = true))
        advanceUntilIdle()

        viewModel.submitAnswer("3")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lastSubmissionCorrect == true)
        assertEquals(1000, viewModel.uiState.value.userElo)
        assertEquals(0, viewModel.uiState.value.sessionNetEloDelta)
    }

    @Test
    fun testEndPractice_ConcludesSessionCleanly() = runTest {
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any(), any()) } returns testQuestion

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository, userPreferencesRepository)
        viewModel.startSession(PracticeSessionConfig(questionCount = 5))
        advanceUntilIdle()

        viewModel.endPractice()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSessionCompleted)
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
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any(), any()) } returns proofQuestion
        coEvery { matchmakingRepository.submitAnswer(proofQuestion, "SOLVED", any(), any(), any()) } returns Pair(
            true,
            EloCalculationResult(1000, 1000, 0, 0.5, true, 1450)
        )

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository, userPreferencesRepository)
        viewModel.loadNextQuestion(proofQuestion.topic)
        advanceUntilIdle()

        viewModel.handleProofEvaluation(true)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isProofSolutionRevealed)
        assertEquals(1, viewModel.uiState.value.sessionSolvedCount)
        assertEquals(0, viewModel.uiState.value.sessionNetEloDelta)
    }

    @Test
    fun testLoadNextQuestion_ExhaustedQuestionsTriggersCompletionDialog() = runTest {
        coEvery { matchmakingRepository.getNextPracticeQuestion(any(), any(), any()) } returns null

        val viewModel = PracticeViewModel(matchmakingRepository, aiTutorRepository, userPreferencesRepository)

        var emittedEvent: UiEvent? = null
        val job = launch {
            emittedEvent = viewModel.uiEvent.first()
        }

        viewModel.loadNextQuestion(testQuestion.topic)
        advanceUntilIdle()

        assertTrue(emittedEvent is UiEvent.ShowCompletionDialog)
        val dialogEvent = emittedEvent as UiEvent.ShowCompletionDialog
        assertEquals("You have conquered this tier! No more questions available right now.", dialogEvent.message)
        job.cancel()
    }
}
