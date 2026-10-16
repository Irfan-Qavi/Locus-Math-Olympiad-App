package com.locus.olympiad.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.locus.olympiad.LocusApplication
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.databinding.ActivityDashboardBinding
import com.locus.olympiad.ui.practice.PracticeActivity
import com.locus.olympiad.ui.theory.TheoryActivity
import com.locus.olympiad.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels {
        (application as LocusApplication).viewModelFactory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnStartPractice.setOnClickListener {
            openPractice(MathTopic.COMBINATORICS)
        }

        binding.btnOpenTheory.setOnClickListener {
            startActivity(Intent(this, TheoryActivity::class.java))
        }

        binding.cardCombinatorics.setOnClickListener { openPractice(MathTopic.COMBINATORICS) }
        binding.cardAlgebra.setOnClickListener { openPractice(MathTopic.ALGEBRA) }
        binding.cardNumberTheory.setOnClickListener { openPractice(MathTopic.NUMBER_THEORY) }
        binding.cardGeometry.setOnClickListener { openPractice(MathTopic.GEOMETRY) }
    }

    private fun openPractice(topic: MathTopic) {
        val intent = Intent(this, PracticeActivity::class.java).apply {
            putExtra(PracticeActivity.EXTRA_CATEGORY, topic.displayName)
        }
        startActivity(intent)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
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
}
