package com.example.zentap

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class GuardAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: OverlayManager
    private val handler = Handler(Looper.getMainLooper())

    private var sessionGrantedUntilMs = 0L
    private var sessionGrantedPackage: String? = null

    private var lastKnownForegroundPackage: String? = null
    private val systemPackages = mutableSetOf("com.android.systemui", "android")

    private val reblockRunnable = Runnable {
        sessionGrantedUntilMs = 0L
        sessionGrantedPackage = null
        val pkg = lastKnownForegroundPackage ?: return@Runnable
        if (GuardedAppsStore.isGuarded(this, pkg) && !overlayManager.isShowing()) {
            showOverlay(pkg)
        }
    }

    override fun onServiceConnected() {
        overlayManager = OverlayManager(this)
        systemPackages.add(packageName)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg: String = when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ->
                event.packageName?.toString() ?: return
            AccessibilityEvent.TYPE_WINDOWS_CHANGED ->
                // TYPE_WINDOWS_CHANGED doesn't carry packageName — ask the active window
                rootInActiveWindow?.packageName?.toString() ?: return
            else -> return
        }

        if (pkg !in systemPackages) {
            lastKnownForegroundPackage = pkg
        }

        if (GuardedAppsStore.isGuarded(this, pkg) && !overlayManager.isShowing()) {
            val sessionActive = pkg == sessionGrantedPackage &&
                System.currentTimeMillis() < sessionGrantedUntilMs
            if (!sessionActive) showOverlay(pkg)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::overlayManager.isInitialized) overlayManager.hide()
    }

    private fun showOverlay(forPackage: String) {
        val appName = GuardedAppsStore.ALL_APPS
            .firstOrNull { it.packageName == forPackage }?.displayName ?: forPackage
        overlayManager.show(
            appName = appName,
            onGrant = { minutes ->
                val ms = minutes * 60 * 1_000L
                sessionGrantedUntilMs = System.currentTimeMillis() + ms
                sessionGrantedPackage = forPackage
                handler.removeCallbacks(reblockRunnable)
                handler.postDelayed(reblockRunnable, ms)
            },
            onNotNow = { performGlobalAction(GLOBAL_ACTION_HOME) }
        )
    }
}
