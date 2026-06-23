package com.example.zentap

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.graphics.drawable.GradientDrawable
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
import android.widget.ProgressBar
import android.widget.ScrollView
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

    // Conversation history for the current overlay session
    private val history = mutableListOf<ChatMessage>()

    // Tracks which intervention to show next; persists across overlay shows for variety
    private var redirectCount = 0

    fun isShowing() = overlayView != null

    fun show(onGrant: (minutes: Int) -> Unit, onNotNow: () -> Unit) {
        if (isShowing()) return

        history.clear()
        scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        val view = inflate(R.layout.overlay_block)
        val layoutMessages = view.findViewById<LinearLayout>(R.id.layout_messages)
        val scrollChat    = view.findViewById<ScrollView>(R.id.scroll_chat)
        val pbLoading     = view.findViewById<ProgressBar>(R.id.pb_loading)
        val etReason      = view.findViewById<EditText>(R.id.et_reason)
        val btnSend       = view.findViewById<Button>(R.id.btn_send)

        // Guardian Mom opens the conversation
        addBubble(layoutMessages, scrollChat, AnthropicClient.OPENING_LINES.random(), isUser = false)

        btnSend.setOnClickListener {
            val text = etReason.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            etReason.text.clear()
            dismissKeyboard(etReason)
            addBubble(layoutMessages, scrollChat, text, isUser = true)
            history.add(ChatMessage("user", text))

            btnSend.isEnabled = false
            pbLoading.visibility = View.VISIBLE

            scope.launch {
                val response = AnthropicClient.chat(context, history)
                // Add assistant reply to history so subsequent turns have full context
                history.add(ChatMessage("assistant", response.reply))

                pbLoading.visibility = View.GONE
                btnSend.isEnabled = true

                when (response.decision) {
                    "grant" -> {
                        addBubble(layoutMessages, scrollChat, response.reply, isUser = false)
                        val userReason = history.lastOrNull { it.role == "user" }?.content?.take(80) ?: ""
                        AccessLogger.log(context, AccessLog(System.currentTimeMillis(), true, response.minutes, userReason))
                        kotlinx.coroutines.delay(1_200)
                        hide()
                        onGrant(response.minutes)
                    }
                    "redirect" -> {
                        addBubble(layoutMessages, scrollChat, response.reply, isUser = false)
                        val userReason = history.lastOrNull { it.role == "user" }?.content?.take(80) ?: ""
                        kotlinx.coroutines.delay(1_500)
                        showReflect(
                            message      = response.reply,
                            onSkip       = {
                                AccessLogger.log(context, AccessLog(System.currentTimeMillis(), false, 0, userReason))
                                hide(); onNotNow()
                            },
                            onOpenAnyway = {
                                AccessLogger.log(context, AccessLog(System.currentTimeMillis(), true, 5, "Opened anyway after redirect"))
                                hide(); onGrant(5)
                            }
                        )
                    }
                    else -> {
                        // "pending" — just show the reply and keep chatting
                        addBubble(layoutMessages, scrollChat, response.reply, isUser = false)
                    }
                }
            }
        }

        view.findViewById<TextView>(R.id.tv_not_now).setOnClickListener {
            AccessLogger.log(context, AccessLog(System.currentTimeMillis(), false, 0, "Skipped without engaging"))
            hide()
            onNotNow()
        }

        windowManager.addView(view, buildParams())
        overlayView = view
    }

    private fun showReflect(message: String, onSkip: () -> Unit, onOpenAnyway: () -> Unit) {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }

        val intervention = nextIntervention()
        redirectCount++

        val view = inflate(R.layout.overlay_reflect)
        view.findViewById<TextView>(R.id.tv_reflect_message).text = message
        view.findViewById<TextView>(R.id.tv_intervention_label).text = intervention.label
        view.findViewById<TextView>(R.id.tv_intervention_body).text = intervention.body

        val btnAction = view.findViewById<Button>(R.id.btn_intervention_action)
        if (intervention.url != null) {
            btnAction.visibility = View.VISIBLE
            btnAction.text = "▶  Open on YouTube"
            btnAction.setOnClickListener {
                AccessLogger.log(context, AccessLog(System.currentTimeMillis(), false, 0, "Redirected to: ${intervention.body.take(60)}"))
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(intervention.url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                hide()
            }
        }

        view.findViewById<Button>(R.id.btn_skip).setOnClickListener { onSkip() }
        view.findViewById<TextView>(R.id.tv_open_anyway).setOnClickListener { onOpenAnyway() }

        windowManager.addView(view, buildParams())
        overlayView = view
    }

    private data class InterventionContent(val label: String, val body: String, val url: String?)

    private fun nextIntervention(): InterventionContent {
        val categoryIndex = redirectCount / 3
        return when (redirectCount % 3) {
            0 -> {
                val items = InterventionData.reflectionQuestions
                InterventionContent("💭  Reflect", items[categoryIndex % items.size], null)
            }
            1 -> {
                val items = InterventionData.youtubeVideos
                val video = items[categoryIndex % items.size]
                InterventionContent("▶  Watch a TED Talk", video.title, video.url)
            }
            else -> {
                val items = InterventionData.physicalNudges
                val nudge = items[categoryIndex % items.size]
                InterventionContent("🏃  ${nudge.title}", nudge.instruction, null)
            }
        }
    }

    fun hide() {
        overlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            overlayView = null
        }
        scope.cancel()
        history.clear()
    }

    // ── Chat bubble helpers ────────────────────────────────────────────────

    private fun addBubble(container: LinearLayout, scroll: ScrollView, text: String, isUser: Boolean) {
        val density = context.resources.displayMetrics.density

        val tv = TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            val pad = (12 * density).toInt()
            setPadding(pad, (8 * density).toInt(), pad, (8 * density).toInt())
            background = bubbleDrawable(isUser)
            // Cap bubble width at ~80% of screen so long messages don't span edge to edge
            maxWidth = (context.resources.displayMetrics.widthPixels * 0.78f).toInt()
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (isUser) Gravity.END else Gravity.START
            val vMargin = (4 * density).toInt()
            val hMargin = (8 * density).toInt()
            if (isUser) setMargins(hMargin * 4, vMargin, hMargin, vMargin)
            else        setMargins(hMargin, vMargin, hMargin * 4, vMargin)
        }

        container.addView(tv, params)
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun bubbleDrawable(isUser: Boolean) = GradientDrawable().apply {
        setColor(if (isUser) 0xFF5B21B6.toInt() else 0xFF2D2B4E.toInt())
        val r = 24f * context.resources.displayMetrics.density
        // Sharpen the corner that points toward the chat edge
        cornerRadii = if (isUser)
            floatArrayOf(r, r, 4f, 4f, r, r, r, r)   // top-right sharp
        else
            floatArrayOf(4f, 4f, r, r, r, r, r, r)    // top-left sharp
    }

    // ── Layout / window helpers ────────────────────────────────────────────

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
            0,
            PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
    }

    private fun dismissKeyboard(view: View) {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(view.windowToken, 0)
    }
}
