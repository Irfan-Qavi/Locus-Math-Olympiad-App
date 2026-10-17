package com.locus.olympiad.ui.practice

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.locus.olympiad.R
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeSessionConfig
import com.locus.olympiad.data.model.QuestionType
import com.locus.olympiad.databinding.ActivityPracticeBinding
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

        val config = (arguments?.getSerializable(EXTRA_CONFIG) as? PracticeSessionConfig)
            ?: run {
                val topicName = arguments?.getString("category")
                val topic = topicName?.let { MathTopic.fromString(it) }
                PracticeSessionConfig(topic = topic)
            }

        setupListeners()
        observeState()

        if (savedInstanceState == null) {
            viewModel.startSession(config)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            navigateBack()
        }

        binding.btnRevealHint.setOnClickListener {
            viewModel.revealNextHint()
        }

        binding.btnSubmitAnswer.setOnClickListener {
            val answer = binding.etAnswer.text?.toString().orEmpty()
            viewModel.submitAnswer(answer)
        }

        // Subjective Type B (Prove That) Listeners
        binding.btnSolveProof.setOnClickListener {
            viewModel.revealProofSolution()
        }

        binding.btnProofValid.setOnClickListener {
            viewModel.submitProofSelfEvaluation(true)
        }

        binding.btnProofIncomplete.setOnClickListener {
            viewModel.submitProofSelfEvaluation(false)
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

        // Session Config & User Rating Badge
        val topic = state.currentQuestion?.topic ?: state.sessionConfig.topic
        binding.tvTopicTitle.text = topic?.displayName ?: state.sessionConfig.topicDisplayName
        binding.tvEloBadge.text = "${state.userElo} [Master]"
        binding.tvSessionProgress.text = state.sessionProgressText

        // Question Details & CMU Serif KaTeX WebView Rendering
        state.currentQuestion?.let { question ->
            binding.tvProblemTag.text = "PROBLEM #${question.id}"
            binding.tvProblemTier.text = question.tier

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
                    binding.containerAnswerInput.isVisible = true
                    binding.btnSolveProof.isVisible = false
                    binding.cardProofSolution.isVisible = false
                    binding.tvAnswerFormatNote.isVisible = true
                    binding.tvAnswerFormatNote.text = "Answer must be strictly between 0 and 99"
                    binding.etAnswer.inputType = InputType.TYPE_CLASS_NUMBER
                    binding.containerQuickKeys.isVisible = false
                }
                QuestionType.SUBJECTIVE_SHORT -> {
                    binding.containerAnswerInput.isVisible = true
                    binding.btnSolveProof.isVisible = false
                    binding.cardProofSolution.isVisible = false
                    binding.tvAnswerFormatNote.isVisible = true
                    binding.tvAnswerFormatNote.text = "Semantic AI evaluation enabled. Phrasing variations accepted."
                    binding.etAnswer.inputType = InputType.TYPE_CLASS_TEXT
                    binding.containerQuickKeys.isVisible = false
                }
                QuestionType.SUBJECTIVE_PROOF -> {
                    binding.containerAnswerInput.isVisible = false
                    binding.tvAnswerFormatNote.isVisible = false
                    binding.btnSolveProof.isVisible = !state.isProofSolutionRevealed
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
                binding.webViewProblemMath.setLatexContent(question.latexContent)
            }
        }

        // Progressive Hints Rendering
        binding.containerRevealedHints.removeAllViews()
        state.visibleHints.forEachIndexed { index, hintText ->
            val hintView = LayoutInflater.from(requireContext()).inflate(
                R.layout.item_revealed_hint,
                binding.containerRevealedHints,
                false
            )
            hintView.findViewById<TextView>(R.id.tvHintTitle).text = "Hint ${index + 1} • Conceptual Lemma"
            hintView.findViewById<TextView>(R.id.tvHintContent).text = hintText
            binding.containerRevealedHints.addView(hintView)
        }

        // Reveal Hint Button State
        if (state.hasMoreHints) {
            val nextHintNum = state.revealedHintsCount + 1
            binding.btnRevealHint.isVisible = true
            binding.btnRevealHint.text = "Reveal Hint $nextHintNum (${state.totalHintsCount - state.revealedHintsCount} remaining)"
        } else {
            binding.btnRevealHint.isVisible = false
        }

        // Draft Answer synchronization
        if (binding.etAnswer.text.toString() != state.userDraftAnswer) {
            binding.etAnswer.setText(state.userDraftAnswer)
            binding.etAnswer.setSelection(state.userDraftAnswer.length)
        }

        // AI Tutor Diagnostic Stepping Stone (Hard-Stuck Trigger)
        if (state.isHardStuck || state.aiTutorHint != null) {
            binding.cardAiTutorHint.isVisible = true
            binding.progressAiTutor.isVisible = state.isLoadingAiHint
            binding.tvAiTutorHint.text = state.aiTutorHint ?: "Locus AI Tutor is diagnosing your 3 incorrect attempts..."
        } else {
            binding.cardAiTutorHint.isVisible = false
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
                binding.tvEvaluationEloDelta.text = if (state.currentQuestion?.resolvedType == QuestionType.SUBJECTIVE_PROOF) {
                    "Neutral Rating (0 Elo Delta)"
                } else {
                    "$deltaSign Elo Rating (Now ${state.userElo})"
                }
                binding.tvEvaluationMessage.text = state.feedbackMessage ?: "Your solution matches the official Olympiad answer key. Outstanding precision."
                binding.btnEvaluationNext.isVisible = true
                binding.btnEvaluationRetry.isVisible = false
            } else {
                binding.ivEvaluationStatus.setImageResource(R.drawable.ic_cancel_circle)
                binding.tvEvaluationTitle.text = if (state.currentQuestion?.resolvedType == QuestionType.SUBJECTIVE_PROOF) "Proof Under Review" else "Incorrect Answer"
                val delta = state.lastEloDelta ?: 0
                val deltaSign = if (delta >= 0) "+$delta" else "$delta"
                binding.tvEvaluationEloDelta.text = if (state.currentQuestion?.resolvedType == QuestionType.SUBJECTIVE_PROOF) "Neutral Rating (0 Elo Delta)" else "$deltaSign Elo Rating"
                binding.tvEvaluationMessage.text = state.feedbackMessage ?: "Re-examine your lemmas and calculations. Keep trying!"
                binding.btnEvaluationNext.isVisible = state.currentQuestion?.resolvedType == QuestionType.SUBJECTIVE_PROOF
                binding.btnEvaluationRetry.isVisible = state.currentQuestion?.resolvedType != QuestionType.SUBJECTIVE_PROOF
            }
        } else {
            binding.overlayEvaluationResult.isVisible = false
        }

        // Session Complete Overlay
        if (state.isSessionCompleted) {
            binding.overlaySessionComplete.isVisible = true
            binding.tvSessionSummary.text = "You solved ${state.sessionSolvedCount} out of ${state.sessionConfig.questionCount} problems."
            val netDelta = state.sessionNetEloDelta
            val netSign = if (netDelta >= 0) "+$netDelta" else "$netDelta"
            binding.tvSessionNetElo.text = "Net Rating: $netSign Elo"
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
