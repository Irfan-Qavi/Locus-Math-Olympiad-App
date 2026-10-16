package com.locus.olympiad.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.locus.olympiad.R
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.databinding.ActivityDashboardBinding
import com.locus.olympiad.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnStartPractice.setOnClickListener {
            openPractice(MathTopic.COMBINATORICS)
        }

        binding.btnOpenTheory.setOnClickListener {
            findNavController().navigate(R.id.nav_theory)
        }

        binding.cardCombinatorics.setOnClickListener { openPractice(MathTopic.COMBINATORICS) }
        binding.cardAlgebra.setOnClickListener { openPractice(MathTopic.ALGEBRA) }
        binding.cardNumberTheory.setOnClickListener { openPractice(MathTopic.NUMBER_THEORY) }
        binding.cardGeometry.setOnClickListener { openPractice(MathTopic.GEOMETRY) }
    }

    private fun openPractice(topic: MathTopic) {
        val bundle = bundleOf("category" to topic.displayName)
        findNavController().navigate(R.id.nav_practice, bundle)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvOverallElo.text = "%,d".format(state.overallElo)
                    binding.tvMasterBadge.text = "${state.overallElo} [Master]"

                    binding.tvCombinatoricsElo.text = "${state.combinatoricsElo} Elo"
                    binding.tvAlgebraElo.text = "${state.algebraElo} Elo"
                    binding.tvNumberTheoryElo.text = "${state.numberTheoryElo} Elo"
                    binding.tvGeometryElo.text = "${state.geometryElo} Elo"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
