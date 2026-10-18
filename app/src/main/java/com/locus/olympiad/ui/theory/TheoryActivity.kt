package com.locus.olympiad.ui.theory

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.chip.Chip
import com.locus.olympiad.R
import com.locus.olympiad.data.model.TheoryModule
import com.locus.olympiad.databinding.ActivityTheoryBinding
import com.locus.olympiad.viewmodel.TheoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TheoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTheoryBinding
    private val viewModel: TheoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTheoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackTheory.setOnClickListener { finish() }

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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var lastRenderedCategory: String? = null
                var lastRenderedModuleId: Long? = null

                viewModel.uiState.collect { state ->
                    if (lastRenderedCategory != state.category) {
                        lastRenderedCategory = state.category
                        lastRenderedModuleId = null
                        populateLemmaChips(state.modules, state.selectedModule)
                    }

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
            val chip = Chip(this).apply {
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
}
