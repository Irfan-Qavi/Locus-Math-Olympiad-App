package com.locus.olympiad.ui.practice

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.locus.olympiad.R
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeSessionConfig
import com.locus.olympiad.data.model.QuestionType
import com.locus.olympiad.databinding.ActivityPracticeBinding
import com.locus.olympiad.ui.main.MainActivity
import com.locus.olympiad.viewmodel.PracticeUiState
import com.locus.olympiad.viewmodel.PracticeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PracticeFragment : Fragment() {

    private var _binding: ActivityPracticeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PracticeViewModel by viewModels()

    private var currentRenderedQuestionId: Long? = null
    private var currentRenderedProofId: Long? = null
    private var currentRenderedRevealedId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityPracticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val shouldResume = arguments?.getBoolean("resume_session", false) ?: false

        setupListeners()
        observeState()

        if (savedInstanceState == null) {
            if (shouldResume) {
                viewModel.resumeSession()
            } else {
                val config = (arguments?.getSerializable(EXTRA_CONFIG) as? PracticeSessionConfig)
                    ?: run {
                        val topicName = arguments?.getString("category")
                        val topic = topicName?.let { MathTopic.fromString(it) }
                        PracticeSessionConfig(topic = topic)
                    }
                viewModel.startSession(config)
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            showEndPracticeDialog { navigateBack() }
        }

        binding.btnEndPractice.setOnClickListener {
            showEndPracticeDialog { navigateBack() }
        }

        binding.btnSubmitAnswer.setOnClickListener {
            val answer = binding.etAnswer.text?.toString().orEmpty()
            viewModel.submitAnswer(answer)
        }

        // Feature 1: Show Answer
        binding.btnShowAnswer.setOnClickListener {
            viewModel.showAnswer()
        }

        binding.btnRevealedNext.setOnClickListener {
            viewModel.proceedToNextQuestion()
        }

        // Feature 4: Streamlined Subjective Proof Choices
        binding.btnProofSolved.setOnClickListener {
            viewModel.handleProofEvaluation(true)
        }

        binding.btnProofUnsolved.setOnClickListener {
            viewModel.handleProofEvaluation(false)
        }

        binding.btnProofNext.setOnClickListener {
            viewModel.proceedToNextQuestion()
        }

        binding.overlayEvaluationResult.setOnClickListener {
            viewModel.dismissEvaluationResult()
        }

        // Evaluation Result Modal Actions
        binding.btnEvaluationNext.setOnClickListener {
            viewModel.dismissEvaluationResult()
            viewModel.proceedToNextQuestion()
        }

        binding.btnEvaluationRetry.setOnClickListener {
            viewModel.dismissEvaluationResult()
            binding.etAnswer.requestFocus()
        }

        // Session Complete Action
        binding.btnFinishSession.setOnClickListener {
            navigateBack()
        }

        // Quick Syntax Keypads
        binding.btnKey1.setOnClickListener { viewModel.appendToDraftAnswer("\\chi(G)") }
        binding.btnKey2.setOnClickListener { viewModel.appendToDraftAnswer("\\operatorname{deg}(v)") }
        binding.btnKey3.setOnClickListener { viewModel.appendToDraftAnswer(" \\pmod 2") }
        binding.btnKey4.setOnClickListener { viewModel.appendToDraftAnswer("\\lceil x \\rceil") }
        binding.btnKey5.setOnClickListener { viewModel.appendToDraftAnswer(" \\le ") }
        binding.btnKey6.setOnClickListener { viewModel.appendToDraftAnswer(" \\ge ") }
        binding.btnKey7.setOnClickListener { viewModel.appendToDraftAnswer("\\infty") }
        binding.btnKeyClear.setOnClickListener {
            viewModel.clearDraftAnswer()
            binding.etAnswer.text?.clear()
        }
    }

    fun endPracticeFromHost() {
        viewModel.endPractice()
    }

    private fun showEndPracticeDialog(pendingNavigation: (() -> Unit)? = null) {
        val host = activity as? MainActivity
        if (host != null) {
            host.showEndPracticeDialog(pendingNavigation)
        } else {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("End Practice Session?")
                .setMessage("Are you sure you want to end this practice session? Your progress will be concluded with 0 penalty.")
                .setPositiveButton("End Practice") { dialog, _ ->
                    dialog.dismiss()
                    viewModel.endPractice()
                    pendingNavigation?.invoke()
                }
                .setNegativeButton("Continue Practicing") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun navigateBack() {
        if (!findNavController().navigateUp()) {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: PracticeUiState) {
        if (_binding == null) return

        binding.progressLoading.isVisible = state.isLoadingQuestion || state.isEvaluatingAnswer
        binding.btnSubmitAnswer.isEnabled = !state.isEvaluatingAnswer

        // Session Config & Dynamic User Rating Badge (Bug 2 fix)
        val topic = state.currentQuestion?.topic ?: state.sessionConfig.topic
        binding.tvTopicTitle.text = topic?.displayName ?: state.sessionConfig.topicDisplayName
        binding.tvEloBadge.text = "${state.userElo} [Master]"
        binding.tvSessionProgress.text = state.sessionProgressText

        // Question Details & CMU Serif KaTeX WebView Rendering
        state.currentQuestion?.let { question ->
            // Question Type Badge
            val qType = question.resolvedType
            binding.tvQuestionTypeBadge.text = when (qType) {
                QuestionType.OBJECTIVE -> "OBJECTIVE [0-99]"
                QuestionType.SUBJECTIVE_SHORT -> "SHORT ANSWER"
                QuestionType.SUBJECTIVE_PROOF -> "PROVE THAT"
            }

            // Optional Source Attribution
            val src = question.source ?: question.sourceContest
            if (!src.isNullOrBlank()) {
                binding.tvProblemSource.isVisible = true
                binding.tvProblemSource.text = "• $src"
            } else {
                binding.tvProblemSource.isVisible = false
            }

            // Split UI Engine based on QuestionType
            when (qType) {
                QuestionType.OBJECTIVE -> {
                    binding.containerProofChoices.isVisible = false
                    binding.cardProofSolution.isVisible = false

                    if (state.isAnswerRevealed) {
                        binding.containerAnswerInput.isVisible = false
                        binding.cardRevealedAnswer.isVisible = true
                        binding.tvRevealedAnswerValue.text = "Answer: ${question.correctAnswer}"
                        val delta = state.lastEloDelta ?: 0
                        binding.tvRevealedPenaltyNote.text = when {
                            state.lastSubmissionCorrect == true -> "Correct! Verified solution."
                            state.sessionConfig.isRiskFree -> "Risk-Free Mode: 0 Elo penalty applied"
                            else -> "Penalty applied: $delta Elo"
                        }
                        val solutionText = if (!question.proofSolution.isNullOrBlank()) {
                            question.proofSolution
                        } else {
                            "\\textbf{Official Solution:}\n\nThe correct answer is $$${question.correctAnswer}$$."
                        }
                        binding.webViewRevealedMath.isVisible = true
                        if (currentRenderedRevealedId != question.id) {
                            currentRenderedRevealedId = question.id
                            binding.webViewRevealedMath.setLatexContent(solutionText)
                        }
                    } else {
                        binding.containerAnswerInput.isVisible = true
                        binding.cardRevealedAnswer.isVisible = false
                        binding.webViewRevealedMath.isVisible = false
                        binding.tvAnswerFormatNote.isVisible = true
                        binding.tvAnswerFormatNote.text = "Answer must be strictly between 0 and 99"
                        binding.etAnswer.inputType = InputType.TYPE_CLASS_NUMBER
                        binding.containerQuickKeys.isVisible = false
                    }
                }
                QuestionType.SUBJECTIVE_SHORT -> {
                    binding.containerProofChoices.isVisible = false
                    binding.cardProofSolution.isVisible = false

                    if (state.isAnswerRevealed) {
                        binding.containerAnswerInput.isVisible = false
                        binding.cardRevealedAnswer.isVisible = true
                        binding.tvRevealedAnswerValue.text = "Answer: ${question.correctAnswer}"
                        val delta = state.lastEloDelta ?: 0
                        binding.tvRevealedPenaltyNote.text = when {
                            state.lastSubmissionCorrect == true -> "Correct! Verified solution."
                            state.sessionConfig.isRiskFree -> "Risk-Free Mode: 0 Elo penalty applied"
                            else -> "Penalty applied: $delta Elo"
                        }
                        val solutionText = if (!question.proofSolution.isNullOrBlank()) {
                            question.proofSolution
                        } else {
                            "\\textbf{Official Solution:}\n\nThe correct answer is $$${question.correctAnswer}$$."
                        }
                        binding.webViewRevealedMath.isVisible = true
                        if (currentRenderedRevealedId != question.id) {
                            currentRenderedRevealedId = question.id
                            binding.webViewRevealedMath.setLatexContent(solutionText)
                        }
                    } else {
                        binding.containerAnswerInput.isVisible = true
                        binding.cardRevealedAnswer.isVisible = false
                        binding.webViewRevealedMath.isVisible = false
                        binding.tvAnswerFormatNote.isVisible = true
                        binding.tvAnswerFormatNote.text = "Semantic AI evaluation enabled. Phrasing variations accepted."
                        binding.etAnswer.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        binding.containerQuickKeys.isVisible = false
                    }
                }
                QuestionType.SUBJECTIVE_PROOF -> {
                    binding.containerAnswerInput.isVisible = false
                    binding.cardRevealedAnswer.isVisible = false
                    binding.webViewRevealedMath.isVisible = false
                    binding.tvAnswerFormatNote.isVisible = false
                    binding.containerProofChoices.isVisible = !state.isProofSolutionRevealed
                    binding.cardProofSolution.isVisible = state.isProofSolutionRevealed

                    if (state.isProofSolutionRevealed && currentRenderedProofId != question.id) {
                        currentRenderedProofId = question.id
                        val proofText = question.proofSolution ?: question.correctAnswer
                        binding.webViewProofMath.setLatexContent(proofText)
                    }
                }
            }

            if (currentRenderedQuestionId != question.id) {
                currentRenderedQuestionId = question.id
                currentRenderedRevealedId = null
                binding.webViewProblemMath.setLatexContent(question.latexContent)
            }
        }

        // Draft Answer synchronization
        if (binding.etAnswer.text.toString() != state.userDraftAnswer) {
            binding.etAnswer.setText(state.userDraftAnswer)
            binding.etAnswer.setSelection(state.userDraftAnswer.length)
        }

        // Prominent Centered Evaluation Feedback Overlay
        if (state.lastSubmissionCorrect != null) {
            binding.overlayEvaluationResult.isVisible = true
            val isCorrect = state.lastSubmissionCorrect
            if (isCorrect) {
                binding.ivEvaluationStatus.setImageResource(R.drawable.ic_check_circle)
                binding.tvEvaluationTitle.text = "Correct Solution!"
                val delta = state.lastEloDelta ?: 0
                val deltaSign = if (delta >= 0) "+$delta" else "$delta"
                binding.tvEvaluationEloDelta.text = if (state.sessionConfig.isRiskFree || state.currentQuestion?.resolvedType == QuestionType.SUBJECTIVE_PROOF) {
                    "Neutral Rating (0 Elo Delta)"
                } else {
                    "$deltaSign Elo Rating (Now ${state.userElo})"
                }
                binding.tvEvaluationMessage.text = state.feedbackMessage ?: "Your solution matches the official Olympiad answer key. Outstanding precision."
                binding.btnEvaluationNext.isVisible = true
                binding.btnEvaluationNext.text = "Next Problem  →"
                binding.btnEvaluationRetry.isVisible = true
                binding.btnEvaluationRetry.text = "Review Solution"
            } else {
                binding.ivEvaluationStatus.setImageResource(R.drawable.ic_cancel_circle)
                binding.tvEvaluationTitle.text = if (state.currentQuestion?.resolvedType == QuestionType.SUBJECTIVE_PROOF) "Proof Under Review" else "Incorrect Answer"
                val delta = state.lastEloDelta ?: 0
                val deltaSign = if (delta >= 0) "+$delta" else "$delta"
                binding.tvEvaluationEloDelta.text = if (state.sessionConfig.isRiskFree || state.currentQuestion?.resolvedType == QuestionType.SUBJECTIVE_PROOF) "Neutral Rating (0 Elo Delta)" else "$deltaSign Elo Rating"
                binding.tvEvaluationMessage.text = state.feedbackMessage ?: "Re-examine your lemmas and calculations. Keep trying!"
                binding.btnEvaluationNext.isVisible = true
                binding.btnEvaluationNext.text = "Skip Problem  →"
                binding.btnEvaluationRetry.isVisible = state.currentQuestion?.resolvedType != QuestionType.SUBJECTIVE_PROOF
                binding.btnEvaluationRetry.text = "Try Again"
            }
        } else {
            binding.overlayEvaluationResult.isVisible = false
        }

        // Session Complete Overlay
        if (state.isSessionCompleted) {
            binding.overlaySessionComplete.isVisible = true
            binding.tvSessionSummary.text = "You completed ${state.sessionSolvedCount} problem(s)."
            val netDelta = state.sessionNetEloDelta
            val netSign = if (netDelta >= 0) "+$netDelta" else "$netDelta"
            binding.tvSessionNetElo.text = if (state.sessionConfig.isRiskFree) "Rating: Unchanged (Risk-Free Mode)" else "Net Rating: $netSign Elo"
        } else {
            binding.overlaySessionComplete.isVisible = false
        }

        // Feedback / Toast
        state.errorMessage?.let { errorMsg ->
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val EXTRA_CONFIG = "extra_session_config"
    }
}
