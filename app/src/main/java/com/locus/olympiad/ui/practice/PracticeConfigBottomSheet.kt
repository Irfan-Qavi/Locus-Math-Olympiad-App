package com.locus.olympiad.ui.practice

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.locus.olympiad.R
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.data.model.PracticeSessionConfig
import com.locus.olympiad.databinding.DialogPracticeConfigBinding

class PracticeConfigBottomSheet() : BottomSheetDialogFragment() {

    var onConfigSelected: ((PracticeSessionConfig) -> Unit)? = null
    private var initialTopic: MathTopic? = null

    constructor(initialTopic: MathTopic? = null, onConfigSelected: (PracticeSessionConfig) -> Unit) : this() {
        this.initialTopic = initialTopic
        this.onConfigSelected = onConfigSelected
    }

    private var _binding: DialogPracticeConfigBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPracticeConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pre-select topic if passed
        initialTopic?.let { topic ->
            when (topic) {
                MathTopic.COMBINATORICS -> binding.chipTopicCombinatorics.isChecked = true
                MathTopic.ALGEBRA -> binding.chipTopicAlgebra.isChecked = true
                MathTopic.NUMBER_THEORY -> binding.chipTopicNumberTheory.isChecked = true
                MathTopic.GEOMETRY -> binding.chipTopicGeometry.isChecked = true
            }
        } ?: run {
            binding.chipTopicAll.isChecked = true
        }

        binding.btnStartConfiguredPractice.setOnClickListener {
            val selectedTopic: MathTopic? = when (binding.chipGroupTopic.checkedChipId) {
                R.id.chipTopicCombinatorics -> MathTopic.COMBINATORICS
                R.id.chipTopicAlgebra -> MathTopic.ALGEBRA
                R.id.chipTopicNumberTheory -> MathTopic.NUMBER_THEORY
                R.id.chipTopicGeometry -> MathTopic.GEOMETRY
                else -> null // MIX ALL TYPES
            }

            val difficultyTier = when (binding.chipGroupDifficulty.checkedChipId) {
                R.id.chipDiffFoundational -> "Foundational"
                R.id.chipDiffIntermediate -> "Intermediate"
                R.id.chipDiffOlympiad -> "Olympiad"
                R.id.chipDiffAll -> "All"
                else -> "Adaptive"
            }

            val questionCount = when (binding.chipGroupCount.checkedChipId) {
                R.id.chipCount5 -> 5
                R.id.chipCount10 -> 10
                R.id.chipCount15 -> 15
                R.id.chipCountEndless -> 0
                else -> 10
            }

            val config = PracticeSessionConfig(
                topic = selectedTopic,
                difficultyTier = difficultyTier,
                questionCount = questionCount
            )

            onConfigSelected?.invoke(config)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PracticeConfigBottomSheet"

        fun newInstance(
            initialTopic: MathTopic? = null,
            onConfigSelected: (PracticeSessionConfig) -> Unit
        ): PracticeConfigBottomSheet {
            return PracticeConfigBottomSheet(initialTopic, onConfigSelected)
        }
    }
}
