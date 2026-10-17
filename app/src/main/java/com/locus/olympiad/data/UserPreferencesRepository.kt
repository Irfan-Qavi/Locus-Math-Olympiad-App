package com.locus.olympiad.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUserNameSync(): String {
        return prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
    }

    fun getUserName(): Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_USER_NAME) {
                trySend(getUserNameSync())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getUserNameSync())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun setUserName(name: String) {
        val trimmed = name.trim()
        val finalName = if (trimmed.isBlank()) DEFAULT_USER_NAME else trimmed
        prefs.edit()
            .putString(KEY_USER_NAME, finalName)
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
    }

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    companion object {
        private const val PREFS_NAME = "locus_user_prefs"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        const val DEFAULT_USER_NAME = "Olympiad Aspirant"
    }
}
