package com.locus.olympiad.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.locus.olympiad.R
import com.locus.olympiad.data.UserPreferencesRepository
import com.locus.olympiad.databinding.ActivityMainBinding
import com.locus.olympiad.ui.practice.PracticeConfigBottomSheet
import com.locus.olympiad.ui.practice.PracticeFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val currentDestId = navController.currentDestination?.id

            // Check if active practice session is running
            val hasActivePractice = userPreferencesRepository.hasActiveSession()

            // 1. PRACTICE tab click intercepted
            if (item.itemId == R.id.nav_practice) {
                if (hasActivePractice) {
                    showActiveSessionPrompt(
                        onContinue = {
                            navigateToPractice(resume = true)
                        },
                        onStop = {
                            userPreferencesRepository.clearActiveSession()
                            showPracticeConfigBottomSheet()
                        }
                    )
                } else {
                    showPracticeConfigBottomSheet()
                }
                return@setOnItemSelectedListener false
            }

            // 2. Navigating away from Practice while session is active
            if (currentDestId == R.id.nav_practice && hasActivePractice) {
                showEndPracticeDialog(pendingNavigation = {
                    performTabNavigation(item.itemId)
                })
                return@setOnItemSelectedListener false
            }

            // If already on the requested tab, avoid re-entrancy
            if (currentDestId == item.itemId) {
                return@setOnItemSelectedListener true
            }

            performTabNavigation(item.itemId)
            true
        }

        // Keep bottom navigation indicator in sync with destinations
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menu = binding.bottomNavigation.menu
            val item = menu.findItem(destination.id)
            if (item != null && !item.isChecked) {
                item.isChecked = true
            }
        }
    }

    private fun performTabNavigation(targetDestinationId: Int) {
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.nav_dashboard, inclusive = false, saveState = true)
            .setRestoreState(true)
            .build()
        try {
            navController.navigate(targetDestinationId, null, navOptions)
        } catch (_: Exception) {
            navController.navigate(targetDestinationId)
        }
    }

    private fun showPracticeConfigBottomSheet() {
        val sheet = PracticeConfigBottomSheet.newInstance { config ->
            val bundle = bundleOf(
                PracticeFragment.EXTRA_CONFIG to config,
                "category" to config.topicDisplayName
            )
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.nav_dashboard, inclusive = false)
                .build()
            navController.navigate(R.id.nav_practice, bundle, navOptions)
        }
        sheet.show(supportFragmentManager, PracticeConfigBottomSheet.TAG)
    }

    private fun navigateToPractice(resume: Boolean) {
        val bundle = bundleOf("resume_session" to resume)
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.nav_dashboard, inclusive = false)
            .build()
        navController.navigate(R.id.nav_practice, bundle, navOptions)
    }

    fun showEndPracticeDialog(pendingNavigation: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(this)
            .setTitle("End Practice Session?")
            .setMessage("Are you sure you want to end this practice session? Your progress will be concluded with 0 penalty.")
            .setPositiveButton("End Practice") { dialog, _ ->
                dialog.dismiss()
                userPreferencesRepository.clearActiveSession()
                val currentDestId = navController.currentDestination?.id
                if (currentDestId == R.id.nav_practice) {
                    val practiceFrag = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                        ?.childFragmentManager?.fragments?.firstOrNull { it is PracticeFragment } as? PracticeFragment
                    practiceFrag?.endPracticeFromHost()
                }
                pendingNavigation?.invoke()
            }
            .setNegativeButton("Continue Practicing") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    fun showActiveSessionPrompt(onContinue: () -> Unit, onStop: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("A practice session is active")
            .setMessage("You have an active Olympiad practice session in progress. Would you like to continue practicing or stop the session?")
            .setPositiveButton("Continue Practicing") { dialog, _ ->
                dialog.dismiss()
                onContinue()
            }
            .setNegativeButton("Stop Session") { dialog, _ ->
                dialog.dismiss()
                onStop()
            }
            .setCancelable(false)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
