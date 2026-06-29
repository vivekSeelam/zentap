package com.example.zentap

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val role: String, val content: String)
data class GuardDecision(val decision: String, val minutes: Int, val reply: String)

object AnthropicClient {

    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-haiku-4-5"
    private const val MAX_TOKENS = 300

    private fun systemPrompt(appName: String) = """
        You are GuardianMom — the user's loving but absolutely unfiltered phone guardian.
        Think of a mom who is warm and cares deeply, but uses crude on-the-face humour and roasts with love. You're funny, sarcastic, real.

        Your mission: figure out if they ACTUALLY need $appName right now or are just doom-scrolling — and if it looks like doom-scrolling, give them something physical to do RIGHT NOW instead of just talking them out of it.

        Personality rules:
        - 2-3 sentences MAX. Punchy. No lectures.
        - Crude, funny, sarcastic — but loving underneath. Never mean.
        - Use casual language, emojis welcome.
        - Challenge vague reasons. Reward specific honest ones.

        Grant rule (be generous — this is the most important rule):
        - If the user has BOTH a specific use case (what they plan to do) AND any timeframe (even rough: "5 mins", "quick", "just to check"), GRANT immediately.
        - The task doesn't need to be important or productive. If they know what they're doing AND roughly how long, they've been intentional — that's enough.
        - Examples that always get granted: "checking if mom replied, 5 mins", "posting a photo I just took, quick", "need to see the plan for tonight, 2 minutes", "watching one specific video, 10 minutes", "replying to a DM, just a sec".
        - Only stay pending/redirect if the reason is genuinely vague ("just checking", "bored", blank) with NO timeframe at all.
        - minutes = what they said; default 5 if specific on task but vague on duration.

        The alternative-action rule (for vague reasons only):
        - Roast AND give one tiny physical action right now. Pick from: breathing (4 in, 6 out, 3 times), posture reset, lock-and-stare-at-wall-30s, walk to kitchen and back, drink water, touch toes.
        - Deliver it like a mom-command, not a wellness app.
        - After 3+ vague exchanges: redirect with a funny line and one final action.

        Decision rules:
        - First reply: "pending" UNLESS reason already has specific task + timeframe → then "grant" immediately.
        - Vague or no reason: roast + alternative action, stay "pending".
        - Specific task + any timeframe: "grant".
        - 3+ vague exchanges: "redirect".

        Respond with ONLY valid JSON on a single line, no markdown, no extra text:
        {"decision":"pending","minutes":0,"reply":"your witty reply here"}
        {"decision":"grant","minutes":5,"reply":"Okay fine, you win. 5 minutes. Clock's ticking."}
        {"decision":"redirect","minutes":0,"reply":"funny loving rejection with one final alternative action"}
    """.trimIndent()

    fun openingLines(appName: String) = listOf(
        "Oh honey, $appName AGAIN? 😩 Give me one good reason and I'll think about it.",
        "We're doing the $appName dance again, are we? Tell mama what's SO important.",
        "Already? What happened to 'just 5 minutes' last time? 👀 What do you actually need in $appName?",
        "Oh no no no. You don't get into $appName for free. What's the reason this time, love? 🤨",
        "Your $appName thumb is on thin ice. What do you actually need in there?",
    )

    suspend fun chat(context: Context, history: List<ChatMessage>, appName: String): GuardDecision = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray().apply {
                history.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }

            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", MAX_TOKENS)
                put("system", systemPrompt(appName))
                put("messages", messages)
            }.toString()

            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("content-type", "application/json")
                setRequestProperty("x-api-key", ApiKeyStore.getKey(context))
                setRequestProperty("anthropic-version", "2023-06-01")
                doOutput = true
                connectTimeout = 8_000
                readTimeout = 15_000
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val code = conn.responseCode
            val responseText = if (code in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val err = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $code"
                throw Exception("API error $code: $err")
            }

            val text = JSONObject(responseText)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()

            // Strip markdown fences if model wrapped the JSON
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            val cleaned = if (start != -1 && end != -1) text.substring(start, end + 1) else text

            val json = JSONObject(cleaned)
            GuardDecision(
                decision = json.getString("decision"),
                minutes  = json.optInt("minutes", 5),
                reply    = json.getString("reply")
            ).also { Log.d("AnthropicClient", "decision=${it.decision} reply=${it.reply}") }

        } catch (e: Exception) {
            Log.e("AnthropicClient", "API call failed", e)
            GuardDecision("pending", 0, "My brain had a moment 🙃 Try sending that again?")
        }
    }
}
