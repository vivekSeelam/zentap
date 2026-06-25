package com.example.zentap

import android.content.Context
import android.content.pm.PackageManager

data class GuardableApp(
    val packageName: String,
    val displayName: String,
    val emoji: String
)

object GuardedAppsStore {

    private const val PREFS_NAME  = "zentap_prefs"
    private const val KEY_GUARDED = "guarded_packages"

    // Add any new app here — it will appear in the UI automatically if installed
    val ALL_APPS = listOf(
        GuardableApp("com.instagram.android",     "Instagram",   "📸"),
        GuardableApp("com.facebook.katana",        "Facebook",    "👥"),
        GuardableApp("com.reddit.frontpage",       "Reddit",      "🤖"),
        GuardableApp("com.google.android.youtube", "YouTube",     "▶️"),
        GuardableApp("com.zhiliaoapp.musically",   "TikTok",      "🎵"),
        GuardableApp("com.twitter.android",        "X (Twitter)", "🐦"),
        GuardableApp("com.linkedin.android",       "LinkedIn",    "💼"),
        GuardableApp("com.snapchat.android",       "Snapchat",    "👻"),
        GuardableApp("com.pinterest",              "Pinterest",   "📌"),
        GuardableApp("com.instagram.barcelona",    "Threads",     "🧵"),
    )

    /** Only the apps from ALL_APPS that are actually installed on this device. */
    fun installedApps(context: Context): List<GuardableApp> {
        val pm = context.packageManager
        return ALL_APPS.filter { app ->
            try { pm.getPackageInfo(app.packageName, 0); true }
            catch (_: PackageManager.NameNotFoundException) { false }
        }
    }

    fun getGuardedPackages(context: Context): Set<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_GUARDED, setOf("com.instagram.android"))
            ?: setOf("com.instagram.android")

    fun setGuardedPackages(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_GUARDED, packages)
            .apply()
    }

    fun isGuarded(context: Context, packageName: String): Boolean =
        packageName in getGuardedPackages(context)
}
