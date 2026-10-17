package com.locus.olympiad.ui.theory

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.locus.olympiad.R
import com.locus.olympiad.data.dao.TheoryModuleDao
import com.locus.olympiad.data.model.TheoryModule
import com.locus.olympiad.databinding.ActivityTheoryBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class TheoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTheoryBinding

    @Inject
    lateinit var theoryModuleDao: TheoryModuleDao

    private var currentModules: List<TheoryModule> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTheoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBackTheory.setOnClickListener { finish() }

        setupUnitTabs()
        loadCategoryTheory("Combinatorics")
    }

    private fun setupUnitTabs() {
        binding.chipGroupUnits.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chipUnitAlgebra -> "Algebra"
                R.id.chipUnitNumberTheory -> "Number Theory"
                R.id.chipUnitGeometry -> "Geometry"
                else -> "Combinatorics"
            }
            loadCategoryTheory(category)
        }
    }

    private fun loadCategoryTheory(category: String) {
        lifecycleScope.launch {
            val modules = withContext(Dispatchers.IO) {
                theoryModuleDao.getModulesByCategorySync(category)
            }
            currentModules = modules
            populateLemmaChips(modules)
        }
    }

    private fun populateLemmaChips(modules: List<TheoryModule>) {
        binding.chipGroupLemmas.removeAllViews()
        if (modules.isEmpty()) return

        modules.forEachIndexed { index, module ->
            val chip = Chip(this).apply {
                text = module.title
                isCheckable = true
                id = View.generateViewId()
                isChecked = index == 0
                setOnClickListener { displayModule(module) }
            }
            binding.chipGroupLemmas.addView(chip)
        }

        val firstModule = modules.first()
        displayModule(firstModule)
    }

    private fun displayModule(module: TheoryModule) {
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
