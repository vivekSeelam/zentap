package com.example.zentap

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun isShowing() = overlayView != null

    fun show(onGrant: (minutes: Int) -> Unit, onNotNow: () -> Unit) {
        if (isShowing()) return

        // Fresh scope for each overlay lifetime
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        val view = inflate(R.layout.overlay_block)
        val layoutInput = view.findViewById<LinearLayout>(R.id.layout_input)
        val layoutLoading = view.findViewById<LinearLayout>(R.id.layout_loading)
        val etReason = view.findViewById<EditText>(R.id.et_reason)

        view.findViewById<Button>(R.id.btn_send).setOnClickListener {
            val reason = etReason.text.toString()
            dismissKeyboard(etReason)
            layoutInput.visibility = View.GONE
            layoutLoading.visibility = View.VISIBLE

            scope.launch {
                val decision = AnthropicClient.evaluate(reason)
                if (decision.decision == "grant") {
                    hide()
                    onGrant(decision.minutes)
                } else {
                    showReflect(
                        message     = decision.message,
                        onSkip      = { hide(); onNotNow() },
                        onOpenAnyway = { hide(); onGrant(5) }
                    )
                }
            }
        }

        view.findViewById<TextView>(R.id.tv_not_now).setOnClickListener {
            hide()
            onNotNow()
        }

        windowManager.addView(view, buildParams())
        overlayView = view
    }

    private fun showReflect(message: String, onSkip: () -> Unit, onOpenAnyway: () -> Unit) {
        // Swap guard overlay for reflect overlay — scope stays alive
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }

        val view = inflate(R.layout.overlay_reflect)
        view.findViewById<TextView>(R.id.tv_reflect_message).text = message
        view.findViewById<Button>(R.id.btn_skip).setOnClickListener { onSkip() }
        view.findViewById<TextView>(R.id.tv_open_anyway).setOnClickListener { onOpenAnyway() }

        windowManager.addView(view, buildParams())
        overlayView = view
    }

    fun hide() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
        scope.cancel()
    }

    private fun inflate(layoutRes: Int): View =
        LayoutInflater.from(ContextThemeWrapper(context, R.style.Theme_Zentap))
            .inflate(layoutRes, null)

    private fun buildParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Pan the overlay up when the soft keyboard opens so the EditText stays visible
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
    }

    private fun dismissKeyboard(view: View) {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(view.windowToken, 0)
    }
}