package com.example.zentap

import android.content.Context

object ApiKeyStore {

    private const val PREFS_NAME = "zentap_prefs"
    private const val KEY_API    = "anthropic_api_key"

    fun getKey(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API, "") ?: ""

    fun saveKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API, key.trim())
            .apply()
    }

    fun isConfigured(context: Context): Boolean = getKey(context).isNotBlank()
}
