package com.locus.olympiad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeQuestion
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
     * Loads the next question matched to the user's current rating (+/- 150 points).
     */
    fun loadNextQuestion(category: MathTopic) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingQuestion = true, errorMessage = null) }
            try {
                val question = matchmakingRepository.getNextPracticeQuestion(category)
                if (question != null) {
                    _uiState.update { current ->
                        current.copy(
                            currentQuestion = question,
                            isLoadingQuestion = false,
                            // Progressive hint system reset for new problem
                            revealedHintsCount = 0,
                            // Reset incorrect attempts and AI tutor state
                            incorrectAttempts = emptyList(),
                            isHardStuck = false,
                            aiTutorHint = null,
                            lastSubmissionCorrect = null,
                            lastEloDelta = null,
                            userDraftAnswer = "",
                            feedbackMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingQuestion = false,
                            errorMessage = "No questions found for ${category.displayName} in your current rating range."
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
     * When the user clicks "Reveal Hint", increment revealedHintsCount and expose the next hint string.
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
     * Handles candidate solution submission.
     * 1. Validates answer against correct_answer.
     * 2. Computes and updates Elo in Room DB.
     * 3. Hard-stuck check: if incorrect, appends to incorrectAttempts.
     *    If incorrectAttempts.size >= 3, flips isHardStuck to true and automatically triggers AI Tutor API.
     */
    fun submitAnswer(submittedAnswer: String = _uiState.value.userDraftAnswer) {
        val trimmedAnswer = submittedAnswer.trim()
        if (trimmedAnswer.isBlank()) {
            _uiState.update { it.copy(feedbackMessage = "Please enter a candidate solution before submitting.") }
            return
        }

        val question = _uiState.value.currentQuestion ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isEvaluatingAnswer = true, feedbackMessage = null, errorMessage = null) }
            try {
                val (isCorrect, eloResult) = matchmakingRepository.submitAnswer(question, trimmedAnswer)

                if (isCorrect) {
                    _uiState.update { current ->
                        current.copy(
                            userElo = eloResult.newElo,
                            lastSubmissionCorrect = true,
                            lastEloDelta = eloResult.delta,
                            isEvaluatingAnswer = false,
                            feedbackMessage = "Correct! Master Elo increased by +${eloResult.delta} pts (${eloResult.newElo}).",
                            // Reset attempts and stuck state on success
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
                            isEvaluatingAnswer = false,
                            feedbackMessage = "Incorrect (${eloResult.delta} pts). Re-examine your lemmas.",
                            incorrectAttempts = updatedAttempts,
                            isHardStuck = isStuck
                        )
                    }

                    // THE HARD-STUCK TRIGGER:
                    // If incorrectAttempts.size >= 3, automatically trigger AI Tutor API call
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
                        feedbackMessage = "Euler AI Tutor has diagnosed your attempts and generated a conceptual stepping stone."
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
}
