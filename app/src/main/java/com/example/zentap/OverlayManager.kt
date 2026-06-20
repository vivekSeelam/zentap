package com.example.zentap

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun isShowing() = overlayView != null

    fun show(onGrant: () -> Unit = {}, onNotNow: () -> Unit = {}) {
        if (isShowing()) return

        val themedContext = ContextThemeWrapper(context, R.style.Theme_Zentap)
        val view = LayoutInflater.from(themedContext).inflate(R.layout.overlay_block, null)

        @Suppress("DEPRECATION")
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        view.findViewById<Button>(R.id.btn_grant).setOnClickListener {
            hide()
            onGrant()
        }

        view.findViewById<TextView>(R.id.tv_not_now).setOnClickListener {
            hide()
            onNotNow()
        }

        windowManager.addView(view, params)
        overlayView = view
    }

    fun hide() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
                // View was already detached
            }
            overlayView = null
        }
    }
}