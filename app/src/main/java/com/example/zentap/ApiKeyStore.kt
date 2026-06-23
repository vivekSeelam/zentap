package com.example.zentap

import android.content.Context

object ApiKeyStore {

    private const val PREFS_NAME = "zentap_prefs"
    private const val KEY_API    = "anthropic_api_key"

    fun getKey(context: Context): String {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API, "")
        // Prefer user-supplied key; fall back to the compile-time key (dev builds only)
        return if (!stored.isNullOrBlank()) stored else BuildConfig.ANTHROPIC_API_KEY
    }

    fun saveKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API, key.trim())
            .apply()
    }

    fun isConfigured(context: Context): Boolean =
        getKey(context).isNotBlank()
}
