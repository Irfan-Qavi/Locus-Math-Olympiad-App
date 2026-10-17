package com.locus.olympiad.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.locus.olympiad.data.model.MathTopic
import com.locus.olympiad.databinding.ActivityDashboardBinding
import com.locus.olympiad.ui.onboarding.OnboardingDialog
import com.locus.olympiad.ui.practice.PracticeActivity
import com.locus.olympiad.ui.practice.PracticeConfigBottomSheet
import com.locus.olympiad.ui.theory.TheoryActivity
import com.locus.olympiad.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkOnboarding()
        setupListeners()
        observeState()
    }

    private fun checkOnboarding() {
        if (!viewModel.hasCompletedOnboarding()) {
            val existing = supportFragmentManager.findFragmentByTag(OnboardingDialog.TAG)
            if (existing == null) {
                val dialog = OnboardingDialog.newInstance { chosenName ->
                    viewModel.setUserName(chosenName)
                }
                dialog.show(supportFragmentManager, OnboardingDialog.TAG)
            }
        }
    }

    private fun setupListeners() {
        binding.btnStartPractice.setOnClickListener {
            openPracticeConfig(null)
        }

        binding.btnOpenTheory.setOnClickListener {
            startActivity(Intent(this, TheoryActivity::class.java))
        }

        binding.cardCombinatorics.setOnClickListener { openPracticeConfig(MathTopic.COMBINATORICS) }
        binding.cardAlgebra.setOnClickListener { openPracticeConfig(MathTopic.ALGEBRA) }
        binding.cardNumberTheory.setOnClickListener { openPracticeConfig(MathTopic.NUMBER_THEORY) }
        binding.cardGeometry.setOnClickListener { openPracticeConfig(MathTopic.GEOMETRY) }
    }

    private fun openPracticeConfig(topic: MathTopic?) {
        val sheet = PracticeConfigBottomSheet.newInstance(topic) { config ->
            val intent = Intent(this, PracticeActivity::class.java).apply {
                putExtra(PracticeActivity.EXTRA_CONFIG, config)
                putExtra(PracticeActivity.EXTRA_CATEGORY, config.topicDisplayName)
            }
            startActivity(intent)
        }
        sheet.show(supportFragmentManager, PracticeConfigBottomSheet.TAG)
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvOverallElo.text = "%,d".format(state.overallElo)
                    binding.tvMasterBadge.text = "${state.overallElo} [${state.rankTitle}]"

                    binding.tvUserInitial.text = state.userInitial
                    binding.tvGreeting.text = "Welcome, ${state.userName}"
                    binding.tvUserRank.text = state.rankTitle

                    binding.tvCombinatoricsElo.text = "${state.combinatoricsElo} Elo"
                    binding.tvAlgebraElo.text = "${state.algebraElo} Elo"
                    binding.tvNumberTheoryElo.text = "${state.numberTheoryElo} Elo"
                    binding.tvGeometryElo.text = "${state.geometryElo} Elo"
                }
            }
        }
    }
}
