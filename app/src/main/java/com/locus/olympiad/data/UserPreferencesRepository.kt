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

    fun saveActiveSession(
        config: com.locus.olympiad.data.model.PracticeSessionConfig,
        questionIndex: Int,
        solvedCount: Int,
        netEloDelta: Int,
        currentQuestionId: Long?
    ) {
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .putString(KEY_SESSION_TOPIC, config.topic?.name)
            .putString(KEY_SESSION_DIFFICULTY, config.difficultyTier)
            .putInt(KEY_SESSION_QUESTION_COUNT, config.questionCount)
            .putBoolean(KEY_SESSION_RISK_FREE, config.isRiskFree)
            .putInt(KEY_SESSION_QUESTION_INDEX, questionIndex)
            .putInt(KEY_SESSION_SOLVED_COUNT, solvedCount)
            .putInt(KEY_SESSION_NET_ELO, netEloDelta)
            .putLong(KEY_SESSION_CURRENT_QUESTION_ID, currentQuestionId ?: -1L)
            .apply()
    }

    fun hasActiveSession(): Boolean {
        return prefs.getBoolean(KEY_SESSION_ACTIVE, false)
    }

    fun getActiveSession(): ActiveSessionState? {
        if (!hasActiveSession()) return null
        val topicName = prefs.getString(KEY_SESSION_TOPIC, null)
        val topic = topicName?.let { runCatching { com.locus.olympiad.data.model.MathTopic.valueOf(it) }.getOrNull() }
        val difficulty = prefs.getString(KEY_SESSION_DIFFICULTY, "Adaptive") ?: "Adaptive"
        val count = prefs.getInt(KEY_SESSION_QUESTION_COUNT, 0)
        val riskFree = prefs.getBoolean(KEY_SESSION_RISK_FREE, false)
        val index = prefs.getInt(KEY_SESSION_QUESTION_INDEX, 1)
        val solved = prefs.getInt(KEY_SESSION_SOLVED_COUNT, 0)
        val netElo = prefs.getInt(KEY_SESSION_NET_ELO, 0)
        val qId = prefs.getLong(KEY_SESSION_CURRENT_QUESTION_ID, -1L).takeIf { it > 0 }

        return ActiveSessionState(
            topic = topic,
            difficultyTier = difficulty,
            questionCount = count,
            isRiskFree = riskFree,
            questionIndex = index,
            solvedCount = solved,
            netEloDelta = netElo,
            currentQuestionId = qId
        )
    }

    fun clearActiveSession() {
        prefs.edit()
            .putBoolean(KEY_SESSION_ACTIVE, false)
            .remove(KEY_SESSION_TOPIC)
            .remove(KEY_SESSION_DIFFICULTY)
            .remove(KEY_SESSION_QUESTION_COUNT)
            .remove(KEY_SESSION_RISK_FREE)
            .remove(KEY_SESSION_QUESTION_INDEX)
            .remove(KEY_SESSION_SOLVED_COUNT)
            .remove(KEY_SESSION_NET_ELO)
            .remove(KEY_SESSION_CURRENT_QUESTION_ID)
            .apply()
    }

    fun getSolvedQuestionIds(): Set<Long> {
        val raw = prefs.getStringSet(KEY_SOLVED_QUESTION_IDS, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun markQuestionSolved(questionId: Long) {
        val current = getSolvedQuestionIds().toMutableSet()
        current.add(questionId)
        prefs.edit()
            .putStringSet(KEY_SOLVED_QUESTION_IDS, current.map { it.toString() }.toSet())
            .apply()
    }

    fun clearSolvedQuestions() {
        prefs.edit().remove(KEY_SOLVED_QUESTION_IDS).apply()
    }

    companion object {
        private const val PREFS_NAME = "locus_user_prefs"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        const val DEFAULT_USER_NAME = "Olympiad Aspirant"

        private const val KEY_SESSION_ACTIVE = "key_session_active"
        private const val KEY_SESSION_TOPIC = "key_session_topic"
        private const val KEY_SESSION_DIFFICULTY = "key_session_difficulty"
        private const val KEY_SESSION_QUESTION_COUNT = "key_session_question_count"
        private const val KEY_SESSION_RISK_FREE = "key_session_risk_free"
        private const val KEY_SESSION_QUESTION_INDEX = "key_session_question_index"
        private const val KEY_SESSION_SOLVED_COUNT = "key_session_solved_count"
        private const val KEY_SESSION_NET_ELO = "key_session_net_elo"
        private const val KEY_SESSION_CURRENT_QUESTION_ID = "key_session_current_question_id"
        private const val KEY_SOLVED_QUESTION_IDS = "key_solved_question_ids"
    }
}

data class ActiveSessionState(
    val topic: com.locus.olympiad.data.model.MathTopic?,
    val difficultyTier: String,
    val questionCount: Int,
    val isRiskFree: Boolean,
    val questionIndex: Int,
    val solvedCount: Int,
    val netEloDelta: Int,
    val currentQuestionId: Long?
)
