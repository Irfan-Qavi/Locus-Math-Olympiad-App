package com.locus.olympiad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeQuestion
import com.locus.olympiad.data.model.PracticeSessionConfig
import com.locus.olympiad.data.model.QuestionType
import com.locus.olympiad.domain.ai.AiTutorRepository
import com.locus.olympiad.repository.MatchmakingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PracticeViewModel @Inject constructor(
    private val matchmakingRepository: MatchmakingRepository,
    private val aiTutorRepository: AiTutorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

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
                isHardStuck = false,
                aiTutorHint = null,
                lastSubmissionCorrect = null,
                lastEloDelta = null,
                isProofSolutionRevealed = false
            )
        }
        loadNextQuestion(config.topic)
    }

    /**
     * Advances to the next question in the session, or marks the session complete
     * if the target problem count has been reached.
     */
    fun proceedToNextQuestion() {
        val config = _uiState.value.sessionConfig
        val currentIndex = _uiState.value.sessionQuestionIndex

        if (!config.isEndless && currentIndex >= config.questionCount) {
            _uiState.update {
                it.copy(
                    isSessionCompleted = true,
                    lastSubmissionCorrect = null,
                    lastEloDelta = null,
                    isProofSolutionRevealed = false
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    sessionQuestionIndex = currentIndex + 1,
                    lastSubmissionCorrect = null,
                    lastEloDelta = null,
                    isProofSolutionRevealed = false
                )
            }
            loadNextQuestion(_uiState.value.sessionConfig.topic)
        }
    }

    /**
     * Loads the next question matched to the user's current rating or difficulty tier.
     */
    fun loadNextQuestion(category: MathTopic? = _uiState.value.sessionConfig.topic) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuestion = true, errorMessage = null) }
            try {
                val difficultyTier = _uiState.value.sessionConfig.difficultyTier
                val question = matchmakingRepository.getNextPracticeQuestion(category, difficultyTier)
                if (question != null) {
                    _uiState.update { current ->
                        current.copy(
                            currentQuestion = question,
                            isLoadingQuestion = false,
                            revealedHintsCount = 0,
                            incorrectAttempts = emptyList(),
                            isHardStuck = false,
                            aiTutorHint = null,
                            lastSubmissionCorrect = null,
                            lastEloDelta = null,
                            userDraftAnswer = "",
                            feedbackMessage = null,
                            isProofSolutionRevealed = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingQuestion = false,
                            errorMessage = "No questions found matching your practice criteria."
                        )
                    }
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
     * Manages the progressive hint revelation system.
     */
    fun revealNextHint() {
        _uiState.update { current ->
            if (current.hasMoreHints) {
                val nextCount = current.revealedHintsCount + 1
                current.copy(
                    revealedHintsCount = nextCount,
                    feedbackMessage = "Hint $nextCount unlocked."
                )
            } else {
                current.copy(feedbackMessage = "All hints have already been revealed.")
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
     * Reveals the full solution and canonical proof for Subjective Type B questions.
     */
    fun revealProofSolution() {
        _uiState.update { it.copy(isProofSolutionRevealed = true) }
    }

    /**
     * Records self-evaluation for Subjective Type B (Prove That) questions.
     * Proof-based questions carry neutral Elo (0 delta).
     */
    fun submitProofSelfEvaluation(isValid: Boolean) {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isEvaluatingAnswer = true) }
            try {
                val (_, eloResult) = matchmakingRepository.submitAnswer(
                    question = question,
                    submittedAnswer = if (isValid) "VALID" else "INCOMPLETE"
                )
                _uiState.update { current ->
                    current.copy(
                        isEvaluatingAnswer = false,
                        lastSubmissionCorrect = isValid,
                        lastEloDelta = 0,
                        sessionSolvedCount = if (isValid) current.sessionSolvedCount + 1 else current.sessionSolvedCount,
                        feedbackMessage = if (isValid) "Proof self-evaluated as valid. Solved count updated." else "Proof marked for review. Neutral Elo preserved."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isEvaluatingAnswer = false,
                        errorMessage = "Self-evaluation failed: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Handles candidate solution submission across Objective, Subjective Short, and Proof questions.
     */
    fun submitAnswer(submittedAnswer: String = _uiState.value.userDraftAnswer) {
        val question = _uiState.value.currentQuestion ?: return

        // If proof question, trigger proof reveal
        if (question.resolvedType == QuestionType.SUBJECTIVE_PROOF) {
            revealProofSolution()
            return
        }

        val trimmedAnswer = submittedAnswer.trim()
        if (trimmedAnswer.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please enter a candidate solution before submitting.") }
            return
        }

        // Strict 0..99 integer validation for Objective questions
        if (question.resolvedType == QuestionType.OBJECTIVE) {
            val intVal = trimmedAnswer.toIntOrNull()
            if (intVal == null || intVal !in 0..99) {
                _uiState.update { it.copy(feedbackMessage = "Answer must be strictly an integer between 0 and 99.") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isEvaluatingAnswer = true, feedbackMessage = null, errorMessage = null) }
            try {
                // Route to AI Tutor semantic evaluation if question is Subjective Short Answer
                val semanticCorrect = if (question.resolvedType == QuestionType.SUBJECTIVE_SHORT) {
                    aiTutorRepository.evaluateSubjectiveAnswer(question, trimmedAnswer).getOrNull()
                } else null

                val (isCorrect, eloResult) = matchmakingRepository.submitAnswer(
                    question = question,
                    submittedAnswer = trimmedAnswer,
                    isSemanticCorrect = semanticCorrect
                )

                if (isCorrect) {
                    _uiState.update { current ->
                        current.copy(
                            userElo = eloResult.newElo,
                            lastSubmissionCorrect = true,
                            lastEloDelta = eloResult.delta,
                            sessionSolvedCount = current.sessionSolvedCount + 1,
                            sessionNetEloDelta = current.sessionNetEloDelta + eloResult.delta,
                            isEvaluatingAnswer = false,
                            feedbackMessage = "Correct! Master Elo increased by +${eloResult.delta} pts (${eloResult.newElo}).",
                            incorrectAttempts = emptyList(),
                            isHardStuck = false,
                            aiTutorHint = null
                        )
                    }
                } else {
                    val updatedAttempts = _uiState.value.incorrectAttempts + trimmedAnswer
                    val isStuck = updatedAttempts.size >= 3

                    _uiState.update { current ->
                        current.copy(
                            userElo = eloResult.newElo,
                            lastSubmissionCorrect = false,
                            lastEloDelta = eloResult.delta,
                            sessionNetEloDelta = current.sessionNetEloDelta + eloResult.delta,
                            isEvaluatingAnswer = false,
                            feedbackMessage = "Incorrect (${eloResult.delta} pts). Re-examine your lemmas.",
                            incorrectAttempts = updatedAttempts,
                            isHardStuck = isStuck
                        )
                    }

                    if (isStuck) {
                        triggerAiTutorHint(question, eloResult.newElo, updatedAttempts)
                    }
                }
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
     * Triggers the AI Tutor API request when hard-stuck.
     */
    fun triggerAiTutorHint(
        question: PracticeQuestion = _uiState.value.currentQuestion!!,
        userElo: Int = _uiState.value.userElo,
        incorrectAttempts: List<String> = _uiState.value.incorrectAttempts
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAiHint = true) }
            val result = aiTutorRepository.getConceptualHint(
                question = question,
                userElo = userElo,
                incorrectAttempts = incorrectAttempts
            )
            result.onSuccess { hintSentence ->
                _uiState.update {
                    it.copy(
                        isLoadingAiHint = false,
                        aiTutorHint = hintSentence,
                        feedbackMessage = "Locus AI Tutor has diagnosed your attempts and generated a conceptual stepping stone."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoadingAiHint = false,
                        errorMessage = "AI Tutor request failed: ${error.localizedMessage}"
                    )
                }
            }
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
}
