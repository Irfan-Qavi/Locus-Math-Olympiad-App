package com.locus.olympiad.ui.theory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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
class TheoryFragment : Fragment() {

    private var _binding: ActivityTheoryBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var theoryModuleDao: TheoryModuleDao

    private var currentModules: List<TheoryModule> = emptyList()

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
        viewLifecycleOwner.lifecycleScope.launch {
            val modules = withContext(Dispatchers.IO) {
                theoryModuleDao.getModulesByCategorySync(category)
            }
            currentModules = modules
            if (_binding != null) {
                populateLemmaChips(modules)
            }
        }
    }

    private fun populateLemmaChips(modules: List<TheoryModule>) {
        binding.chipGroupLemmas.removeAllViews()
        if (modules.isEmpty()) return

        modules.forEachIndexed { index, module ->
            val chip = Chip(requireContext()).apply {
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
