package com.example.zentap

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class GuardAccessibilityService : AccessibilityService() {

    private lateinit var overlayManager: OverlayManager
    private val handler = Handler(Looper.getMainLooper())

    private var sessionGrantedUntilMs = 0L

    // Updated on every window-state event (excluding system UI packages) so we know
    // what's actually in the foreground when the re-block timer fires.
    private var lastKnownForegroundPackage: String? = null

    // Packages whose window events don't represent real app foreground changes
    private val systemPackages = mutableSetOf("com.android.systemui", "android")

    private val reblockRunnable = Runnable {
        sessionGrantedUntilMs = 0L
        if (lastKnownForegroundPackage == GUARDED_PACKAGE && !overlayManager.isShowing()) {
            showOverlay()
        }
    }

    override fun onServiceConnected() {
        overlayManager = OverlayManager(this)
        systemPackages.add(packageName)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        if (pkg !in systemPackages) {
            lastKnownForegroundPackage = pkg
        }

        if (pkg == GUARDED_PACKAGE && !overlayManager.isShowing()) {
            val sessionActive = System.currentTimeMillis() < sessionGrantedUntilMs
            if (!sessionActive) showOverlay()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        if (::overlayManager.isInitialized) overlayManager.hide()
    }

    private fun showOverlay() {
        overlayManager.show(
            onGrant = { minutes ->
                // AI-determined grant duration (5–15 min), then re-block
                val ms = minutes * 60 * 1_000L
                sessionGrantedUntilMs = System.currentTimeMillis() + ms
                handler.removeCallbacks(reblockRunnable)
                handler.postDelayed(reblockRunnable, ms)
            },
            onNotNow = {
                // User tapped "Not now" — send them home; no timer started
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        )
    }

    companion object {
        const val GUARDED_PACKAGE = "com.instagram.android"
    }
}