package com.locus.olympiad.ui.practice

import android.os.Bundle
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

        val topicName = arguments?.getString("category") ?: "Combinatorics"
        val topic = MathTopic.fromString(topicName)

        setupListeners(topic)
        observeState()

        if (savedInstanceState == null) {
            viewModel.loadNextQuestion(topic)
        }
    }

    private fun setupListeners(topic: MathTopic) {
        binding.btnBack.setOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.btnRevealHint.setOnClickListener {
            viewModel.revealNextHint()
        }

        binding.btnSubmitAnswer.setOnClickListener {
            val answer = binding.etAnswer.text?.toString().orEmpty()
            viewModel.submitAnswer(answer)
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

        // User Rating Badge
        val topic = state.currentQuestion?.topic ?: MathTopic.COMBINATORICS
        binding.tvTopicTitle.text = topic.displayName
        binding.tvEloBadge.text = "${state.userElo} [Master]"

        // Question Details & CMU Serif KaTeX WebView Rendering
        state.currentQuestion?.let { question ->
            binding.tvProblemTag.text = "PROBLEM #${question.id}"
            binding.tvProblemTier.text = question.tier

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
            binding.tvAiTutorHint.text = state.aiTutorHint ?: "Euler AI Tutor is diagnosing your 3 incorrect attempts..."
        } else {
            binding.cardAiTutorHint.isVisible = false
        }

        // Feedback / Toast
        state.feedbackMessage?.let { msg ->
            binding.tvFeedback.isVisible = true
            binding.tvFeedback.text = msg
        } ?: run {
            binding.tvFeedback.isVisible = false
        }

        state.errorMessage?.let { errorMsg ->
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
