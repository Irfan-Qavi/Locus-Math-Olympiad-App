package com.locus.olympiad.ui.theory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.locus.olympiad.R
import com.locus.olympiad.data.model.TheoryModule
import com.locus.olympiad.databinding.ActivityTheoryBinding
import com.locus.olympiad.viewmodel.TheoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TheoryFragment : Fragment() {

    private var _binding: ActivityTheoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TheoryViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityTheoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBackTheory.setOnClickListener {
            if (!findNavController().navigateUp()) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        setupUnitTabs()
        observeState()
    }

    private fun setupUnitTabs() {
        binding.chipGroupUnits.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val category = when (checkedId) {
                R.id.chipUnitAlgebra -> "Algebra"
                R.id.chipUnitNumberTheory -> "Number Theory"
                R.id.chipUnitGeometry -> "Geometry"
                R.id.chipUnitCombinatorics -> "Combinatorics"
                else -> "Combinatorics"
            }
            viewModel.loadTheory(category)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var lastRenderedCategory: String? = null
                var lastRenderedModuleId: Long? = null

                viewModel.uiState.collect { state ->
                    // Rebuild chips when the category changes or when modules update
                    if (lastRenderedCategory != state.category) {
                        lastRenderedCategory = state.category
                        lastRenderedModuleId = null
                        populateLemmaChips(state.modules, state.selectedModule)
                    }

                    // Only trigger KaTeX rendering once combined state (Theory Text + Tips/Heuristics) is non-null & ready
                    if (state.isReady && state.selectedModule != null) {
                        if (lastRenderedModuleId != state.selectedModule.id) {
                            lastRenderedModuleId = state.selectedModule.id
                            renderModule(state.selectedModule)
                        }
                    }
                }
            }
        }
    }

    private fun populateLemmaChips(modules: List<TheoryModule>, selected: TheoryModule?) {
        binding.chipGroupLemmas.removeAllViews()
        if (modules.isEmpty()) return

        modules.forEach { module ->
            val chip = Chip(requireContext()).apply {
                text = module.title
                isCheckable = true
                id = View.generateViewId()
                isChecked = (selected != null && module.id == selected.id) || (selected == null && module == modules.first())
                setOnClickListener { viewModel.selectModule(module) }
            }
            binding.chipGroupLemmas.addView(chip)
        }
    }

    private fun renderModule(module: TheoryModule) {
        binding.tvTheoryTitle.text = module.title
        binding.tvChapterHeader.text = module.chapterName.uppercase()
        binding.webViewTheoryMath.setLatexContent(module.latexContent)

        // Dedicated Tips, Tricks & Heuristics Section
        val heuristics = module.heuristicsAndTricks
        if (!heuristics.isNullOrBlank()) {
            binding.cardHeuristics.isVisible = true
            binding.webViewHeuristicsMath.setLatexContent(heuristics)
        } else {
            binding.cardHeuristics.isVisible = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
