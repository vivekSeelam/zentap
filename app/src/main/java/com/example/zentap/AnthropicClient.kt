package com.example.zentap

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class GuardDecision(val decision: String, val minutes: Int, val message: String)

object AnthropicClient {

    private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-haiku-4-5"
    private const val MAX_TOKENS = 200

    private val SYSTEM_PROMPT = """
        You are a mindfulness guardian embedded in a phone app that intercepts impulsive scrolling.
        The user is about to open Instagram. They provided a reason (or none).

        Evaluate strictly and respond with ONLY valid JSON — no other text:

        GRANT if the reason is specific and intentional (e.g. "texting a friend", "posting a photo"):
        {"decision":"grant","minutes":5,"message":"One warm sentence acknowledging their purpose."}

        REDIRECT if the reason is vague, impulsive, or absent (e.g. "just checking", "bored", ""):
        {"decision":"redirect","minutes":0,"message":"One warm sentence gently naming the impulse."}

        For grant: minutes = 5–15 based on how time-bounded the task sounds.
        Keep message under 20 words.
    """.trimIndent()

    suspend fun evaluate(reason: String): GuardDecision = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", MAX_TOKENS)
                put("system", SYSTEM_PROMPT)
                put("messages", JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", reason.ifBlank { "(no reason given)" })
                    }
                ))
            }.toString()

            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("content-type", "application/json")
                setRequestProperty("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
                setRequestProperty("anthropic-version", "2023-06-01")
                doOutput = true
                connectTimeout = 8_000
                readTimeout = 15_000
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val responseCode = conn.responseCode
            val responseText = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().readText()
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                throw Exception("API error $responseCode: $error")
            }

            val text = JSONObject(responseText)
                .getJSONArray("content")
                .getJSONObject(0)
                .getString("text")
                .trim()

            // Strip markdown code fences if the model wrapped the JSON
            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            val cleaned = if (start != -1 && end != -1) text.substring(start, end + 1) else text
            val json = JSONObject(cleaned)
            GuardDecision(
                decision = json.getString("decision"),
                minutes  = json.optInt("minutes", 5),
                message  = json.getString("message")
            ).also { Log.d("AnthropicClient", "decision=${it.decision} minutes=${it.minutes} message=${it.message}") }
        } catch (e: Exception) {
            Log.e("AnthropicClient", "API call failed", e)
            // Fail open — if the API is unreachable the app should not block the user
            GuardDecision("grant", 5, "Guardian unavailable. You have 5 minutes.")
        }
    }
}