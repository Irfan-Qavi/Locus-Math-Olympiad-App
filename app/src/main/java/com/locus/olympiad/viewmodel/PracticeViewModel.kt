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
                    lastEloDelta = null
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
            loadNextQuestion(config.topic)
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
                            incorrectAttempts = emptyList(),
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
     * Reveals the official proof solution for Subjective Proof questions.
     */
    fun revealProofSolution() {
        _uiState.update { it.copy(isProofSolutionRevealed = true) }
    }

    /**
     * Submits self-evaluation for Subjective Proof problems (True = Valid proof, False = Incomplete).
     */
    fun submitProofSelfEvaluation(isValidProof: Boolean) {
        val question = _uiState.value.currentQuestion ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isEvaluatingAnswer = true) }
            try {
                val answerString = if (isValidProof) "PROVED" else "INCOMPLETE"
                val (isCorrect, eloResult) = matchmakingRepository.submitAnswer(
                    question = question,
                    submittedAnswer = answerString
                )

                _uiState.update { current ->
                    current.copy(
                        userElo = eloResult.newElo,
                        lastSubmissionCorrect = isCorrect,
                        lastEloDelta = eloResult.delta,
                        sessionSolvedCount = if (isCorrect) current.sessionSolvedCount + 1 else current.sessionSolvedCount,
                        sessionNetEloDelta = current.sessionNetEloDelta + eloResult.delta,
                        isEvaluatingAnswer = false,
                        feedbackMessage = if (isCorrect) "Proof self-validated as correct." else "Proof marked incomplete for review."
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
                            feedbackMessage = "Incorrect (${eloResult.delta} pts). Re-examine your lemmas.",
                            incorrectAttempts = updatedAttempts
                        )
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
