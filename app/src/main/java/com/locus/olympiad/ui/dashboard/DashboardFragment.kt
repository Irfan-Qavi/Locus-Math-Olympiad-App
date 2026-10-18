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
import com.locus.olympiad.ui.onboarding.OnboardingDialog
import com.locus.olympiad.ui.practice.PracticeConfigBottomSheet
import com.locus.olympiad.ui.practice.PracticeFragment
import com.locus.olympiad.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardFragment : Fragment() {

    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()

    @javax.inject.Inject
    lateinit var userPreferencesRepository: com.locus.olympiad.data.UserPreferencesRepository

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
        checkOnboarding()
        setupListeners()
        observeState()
    }

    private fun checkOnboarding() {
        if (!viewModel.hasCompletedOnboarding()) {
            val existing = childFragmentManager.findFragmentByTag(OnboardingDialog.TAG)
            if (existing == null) {
                val dialog = OnboardingDialog.newInstance { chosenName ->
                    viewModel.setUserName(chosenName)
                }
                dialog.show(childFragmentManager, OnboardingDialog.TAG)
            }
        }
    }

    private fun setupListeners() {
        binding.btnStartPractice.setOnClickListener {
            handleStartPractice(null)
        }

        binding.btnOpenTheory.setOnClickListener {
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_dashboard, inclusive = false)
                .build()
            findNavController().navigate(R.id.nav_theory, null, navOptions)
        }

        binding.cardCombinatorics.setOnClickListener { handleStartPractice(MathTopic.COMBINATORICS) }
        binding.cardAlgebra.setOnClickListener { handleStartPractice(MathTopic.ALGEBRA) }
        binding.cardNumberTheory.setOnClickListener { handleStartPractice(MathTopic.NUMBER_THEORY) }
        binding.cardGeometry.setOnClickListener { handleStartPractice(MathTopic.GEOMETRY) }
    }

    private fun handleStartPractice(topic: MathTopic?) {
        if (userPreferencesRepository.hasActiveSession()) {
            val host = activity as? com.locus.olympiad.ui.main.MainActivity
            if (host != null) {
                host.showActiveSessionPrompt(
                    onContinue = {
                        val bundle = bundleOf("resume_session" to true)
                        val navOptions = androidx.navigation.NavOptions.Builder()
                            .setLaunchSingleTop(true)
                            .setPopUpTo(R.id.nav_dashboard, inclusive = false)
                            .build()
                        findNavController().navigate(R.id.nav_practice, bundle, navOptions)
                    },
                    onStop = {
                        userPreferencesRepository.clearActiveSession()
                        openPracticeConfig(topic)
                    }
                )
            } else {
                openPracticeConfig(topic)
            }
        } else {
            openPracticeConfig(topic)
        }
    }

    private fun openPracticeConfig(topic: MathTopic?) {
        val sheet = PracticeConfigBottomSheet.newInstance(topic) { config ->
            val bundle = bundleOf(
                PracticeFragment.EXTRA_CONFIG to config,
                "category" to config.topicDisplayName
            )
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_dashboard, inclusive = false)
                .build()
            findNavController().navigate(R.id.nav_practice, bundle, navOptions)
        }
        sheet.show(childFragmentManager, PracticeConfigBottomSheet.TAG)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.tvOverallElo.text = "%,d".format(state.overallElo)
                    binding.tvMasterBadge.text = "${state.overallElo} [${state.rankTitle}]"

                    binding.tvUserInitial.text = state.userInitial
                    binding.tvGreeting.text = "Welcome, ${state.userName}"
                    binding.tvUserRank.text = state.rankTitle
                    binding.tvSolvedCount.text = "${state.solvedCount} Solved"

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
