package com.locus.olympiad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.olympiad.data.UserPreferencesRepository
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.PracticeSessionConfig
import com.locus.olympiad.data.model.QuestionType
import com.locus.olympiad.data.model.UserTopicProfile
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.repository.MatchmakingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val matchmakingRepository: MatchmakingRepository,
    private val aiTutorRepository: AiTutorRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<UiEvent> = _uiEvent.receiveAsFlow()

    init {
        observeUserElo()
    }

    /**
     * Dynamically collects user Elo rating from repository/database flow,
     * ensuring header badge updates immediately without desynchronization.
     */
    private fun observeUserElo() {
        viewModelScope.launch {
            matchmakingRepository.getAllUserProfiles().collect { profiles ->
                val topic = _uiState.value.sessionConfig.topic
                val rating = if (topic != null) {
                    profiles.find { it.category.equals(topic.displayName, ignoreCase = true) }?.eloRating
                        ?: UserTopicProfile.DEFAULT_ELO
                } else {
                    if (profiles.isNotEmpty()) profiles.map { it.eloRating }.average().toInt() else UserTopicProfile.DEFAULT_ELO
                }
                _uiState.update { it.copy(userElo = rating) }
            }
        }
    }

    /**
     * Initializes a new practice session with the user-selected configurator options.
     */
    fun startSession(config: PracticeSessionConfig) {
        _uiState.update {
            it.copy(
                sessionConfig = config,
                sessionQuestionIndex = 1,
                sessionSolvedCount = 0,
                sessionNetEloDelta = 0,
                isSessionCompleted = false,
                incorrectAttempts = emptyList(),
                lastSubmissionCorrect = null,
                lastEloDelta = null,
                isProofSolutionRevealed = false,
                isAnswerRevealed = false
            )
        }
        saveCurrentSessionState()
        loadNextQuestion(config.topic)
    }

    /**
     * Resumes an active practice session from persistent storage.
     */
    fun resumeSession() {
        val active = userPreferencesRepository.getActiveSession() ?: return
        val config = PracticeSessionConfig(
            topic = active.topic,
            difficultyTier = active.difficultyTier,
            questionCount = active.questionCount,
            isRiskFree = active.isRiskFree
        )
        _uiState.update {
            it.copy(
                sessionConfig = config,
                sessionQuestionIndex = active.questionIndex,
                sessionSolvedCount = active.solvedCount,
                sessionNetEloDelta = active.netEloDelta,
                isSessionCompleted = false,
                incorrectAttempts = emptyList(),
                lastSubmissionCorrect = null,
                lastEloDelta = null,
                isProofSolutionRevealed = false,
                isAnswerRevealed = false
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuestion = true) }
            val qId = active.currentQuestionId
            val question = if (qId != null) matchmakingRepository.getQuestionById(qId) else null
            if (question != null) {
                _uiState.update { it.copy(currentQuestion = question, isLoadingQuestion = false) }
            } else {
                loadNextQuestion(config.topic)
            }
        }
    }

    /**
     * Advances to the next question in the session, or marks the session complete
     * if the target problem count has been reached.
     */
    fun proceedToNextQuestion() {
        val config = _uiState.value.sessionConfig
        val currentIndex = _uiState.value.sessionQuestionIndex

        if (!config.isEndless && currentIndex >= config.questionCount) {
            userPreferencesRepository.clearActiveSession()
            _uiState.update {
                it.copy(
                    isSessionCompleted = true,
                    lastSubmissionCorrect = null,
                    lastEloDelta = null
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    sessionQuestionIndex = currentIndex + 1,
                    lastSubmissionCorrect = null,
                    lastEloDelta = null,
                    isProofSolutionRevealed = false,
                    isAnswerRevealed = false
                )
            }
            saveCurrentSessionState()
            loadNextQuestion(config.topic)
        }
    }

    /**
     * Loads the next question matched to the user's current rating or difficulty tier.
     * Prevents repetition by querying only questions the user has not yet solved.
     */
    fun loadNextQuestion(category: MathTopic? = _uiState.value.sessionConfig.topic) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuestion = true, errorMessage = null) }
            try {
                val difficultyTier = _uiState.value.sessionConfig.difficultyTier
                val solvedIds = userPreferencesRepository.getSolvedQuestionIds().toList()
                val question = matchmakingRepository.getNextPracticeQuestion(category, difficultyTier, solvedIds)
                if (question != null) {
                    _uiState.update { current ->
                        current.copy(
                            currentQuestion = question,
                            isLoadingQuestion = false,
                            incorrectAttempts = emptyList(),
                            lastSubmissionCorrect = null,
                            lastEloDelta = null,
                            userDraftAnswer = "",
                            feedbackMessage = null,
                            isProofSolutionRevealed = false,
                            isAnswerRevealed = false
                        )
                    }
                    saveCurrentSessionState()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingQuestion = false,
                            errorMessage = null
                        )
                    }
                    _uiEvent.send(UiEvent.ShowCompletionDialog("You have conquered this tier! No more questions available right now."))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingQuestion = false,
                        errorMessage = "Failed to load matchmaking question: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Updates the user's draft answer as they type or tap quick-syntax keys.
     */
    fun updateDraftAnswer(answer: String) {
        _uiState.update { it.copy(userDraftAnswer = answer) }
    }

    /**
     * Appends a mathematical symbol or expression to the candidate evaluation draft.
     */
    fun appendToDraftAnswer(symbol: String) {
        _uiState.update { it.copy(userDraftAnswer = it.userDraftAnswer + symbol) }
    }

    /**
     * Clears the current draft answer.
     */
    fun clearDraftAnswer() {
        _uiState.update { it.copy(userDraftAnswer = "") }
    }

    /**
     * Surrenders the active question, applies a 1.1x incorrect penalty (or 0 if Risk-Free),
     * reveals the official answer, and transitions the UI to show only the "Next Question" button.
     */
    fun showAnswer() {
        val question = _uiState.value.currentQuestion ?: return
        if (_uiState.value.isAnswerRevealed) return

        viewModelScope.launch {
            _uiState.update { it.copy(isEvaluatingAnswer = true) }
            try {
                val isRiskFree = _uiState.value.sessionConfig.isRiskFree
                val (_, eloResult) = matchmakingRepository.submitAnswer(
                    question = question,
                    submittedAnswer = "SHOW_ANSWER",
                    isRiskFree = isRiskFree,
                    isShowAnswerPenalty = true
                )

                _uiState.update { current ->
                    current.copy(
                        userElo = eloResult.newElo,
                        lastSubmissionCorrect = null,
                        lastEloDelta = eloResult.delta,
                        sessionNetEloDelta = current.sessionNetEloDelta + eloResult.delta,
                        isEvaluatingAnswer = false,
                        isAnswerRevealed = true,
                        feedbackMessage = if (isRiskFree) {
                            "Official answer revealed (Risk-Free Mode: 0 Elo penalty applied)."
                        } else {
                            "Official answer revealed (${eloResult.delta} Elo penalty applied)."
                        }
                    )
                }
                saveCurrentSessionState()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isEvaluatingAnswer = false,
                        errorMessage = "Failed to reveal answer: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Handles subjective proof evaluation ("I HAVE SOLVED IT" vs "I COULDN'T SOLVE IT").
     * Subjective proof carries neutral rating (0 Elo delta).
     * Reveals the full proof and displays only the Next Question button.
     */
    fun handleProofEvaluation(isValidProof: Boolean) {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isEvaluatingAnswer = true) }
            try {
                val answerString = if (isValidProof) "SOLVED" else "UNSOLVED"
                val (isCorrect, eloResult) = matchmakingRepository.submitAnswer(
                    question = question,
                    submittedAnswer = answerString,
                    isRiskFree = _uiState.value.sessionConfig.isRiskFree
                )

                if (isValidProof) {
                    userPreferencesRepository.markQuestionSolved(question.id)
                }

                _uiState.update { current ->
                    current.copy(
                        userElo = eloResult.newElo,
                        lastSubmissionCorrect = null,
                        lastEloDelta = 0,
                        sessionSolvedCount = if (isValidProof) current.sessionSolvedCount + 1 else current.sessionSolvedCount,
                        sessionNetEloDelta = current.sessionNetEloDelta + eloResult.delta,
                        isEvaluatingAnswer = false,
                        isProofSolutionRevealed = true,
                        feedbackMessage = if (isValidProof) "Proof marked as solved." else "Official solution revealed for review."
                    )
                }
                saveCurrentSessionState()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isEvaluatingAnswer = false,
                        errorMessage = "Proof evaluation failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun submitProofSelfEvaluation(isValidProof: Boolean) {
        handleProofEvaluation(isValidProof)
    }

    /**
     * Evaluates a student's submitted answer (Objective or Subjective Short).
     */
    fun submitAnswer(candidateAnswer: String) {
        val question = _uiState.value.currentQuestion ?: return
        val trimmedAnswer = candidateAnswer.trim()
        if (trimmedAnswer.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please provide an answer before submitting.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isEvaluatingAnswer = true, feedbackMessage = null) }
            try {
                var semanticCorrect: Boolean? = null
                if (question.resolvedType == QuestionType.SUBJECTIVE_SHORT) {
                    val aiResult = aiTutorRepository.evaluateSubjectiveAnswer(question, trimmedAnswer)
                    semanticCorrect = aiResult.getOrNull()
                }

                val isRiskFree = _uiState.value.sessionConfig.isRiskFree
                val (isCorrect, eloResult) = matchmakingRepository.submitAnswer(
                    question = question,
                    submittedAnswer = trimmedAnswer,
                    isSemanticCorrect = semanticCorrect,
                    isRiskFree = isRiskFree
                )

                if (isCorrect) {
                    userPreferencesRepository.markQuestionSolved(question.id)
                    _uiState.update { current ->
                        current.copy(
                            userElo = eloResult.newElo,
                            lastSubmissionCorrect = true,
                            lastEloDelta = eloResult.delta,
                            sessionSolvedCount = current.sessionSolvedCount + 1,
                            sessionNetEloDelta = current.sessionNetEloDelta + eloResult.delta,
                            isEvaluatingAnswer = false,
                            isAnswerRevealed = true,
                            feedbackMessage = if (isRiskFree) {
                                "Correct! (Risk-Free Mode: 0 Elo delta)."
                            } else {
                                "Correct! Master Elo increased by +${eloResult.delta} pts (${eloResult.newElo})."
                            },
                            incorrectAttempts = emptyList()
                        )
                    }
                } else {
                    val updatedAttempts = _uiState.value.incorrectAttempts + trimmedAnswer
                    _uiState.update { current ->
                        current.copy(
                            userElo = eloResult.newElo,
                            lastSubmissionCorrect = false,
                            lastEloDelta = eloResult.delta,
                            sessionNetEloDelta = current.sessionNetEloDelta + eloResult.delta,
                            isEvaluatingAnswer = false,
                            feedbackMessage = if (isRiskFree) {
                                "Incorrect (Risk-Free Mode: 0 Elo delta). Re-examine your lemmas."
                            } else {
                                "Incorrect (${eloResult.delta} pts). Re-examine your lemmas."
                            },
                            incorrectAttempts = updatedAttempts
                        )
                    }
                }
                saveCurrentSessionState()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isEvaluatingAnswer = false,
                        errorMessage = "Submission evaluation failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Concludes the practice session cleanly with 0 penalty, clears active session state,
     * and reports the session summary overlay.
     */
    fun endPractice() {
        userPreferencesRepository.clearActiveSession()
        _uiState.update {
            it.copy(
                isSessionCompleted = true,
                lastSubmissionCorrect = null,
                lastEloDelta = null
            )
        }
    }

    /**
     * Dismisses the evaluation feedback overlay and allows retrying or reviewing the question.
     */
    fun dismissEvaluationResult() {
        _uiState.update {
            it.copy(
                lastSubmissionCorrect = null,
                lastEloDelta = null
            )
        }
    }

    private fun saveCurrentSessionState() {
        val state = _uiState.value
        if (!state.isSessionCompleted) {
            userPreferencesRepository.saveActiveSession(
                config = state.sessionConfig,
                questionIndex = state.sessionQuestionIndex,
                solvedCount = state.sessionSolvedCount,
                netEloDelta = state.sessionNetEloDelta,
                currentQuestionId = state.currentQuestion?.id
            )
        }
    }
}
