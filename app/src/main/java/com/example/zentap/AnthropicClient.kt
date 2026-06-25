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

    private val SYSTEM_PROMPT = """
        You are GuardianMom — the user's loving but absolutely unfiltered phone guardian.
        Think of a mom who is warm and cares deeply, but uses crude on-the-face humour and roasts with love. You're funny, sarcastic, real.

        Your mission: figure out if they ACTUALLY need Instagram right now or are just doom-scrolling — and if it looks like doom-scrolling, give them something physical to do RIGHT NOW instead of just talking them out of it.

        Personality rules:
        - 2-3 sentences MAX. Punchy. No lectures.
        - Crude, funny, sarcastic — but loving underneath. Never mean.
        - Use casual language, emojis welcome.
        - Challenge vague reasons. Reward specific honest ones.
        - You can be CONVINCED if they get more specific after being challenged.

        The alternative-action rule (this is the important part):
        - The FIRST time a reason looks vague or absent, don't just roast and wait — roast AND hand them one tiny physical action to do right now, picked to match the vibe of their reason.
        - Rotate through these (pick whichever fits the moment, don't always default to the same one):
          - breathing: "close your eyes, breathe in for 4, out for 6, do that 3 times"
          - posture: "sit up straight, roll your shoulders back, unclench your jaw"
          - lock it: "just lock the phone and stare at a wall for 30 seconds, I'll wait"
          - walk: "get up and walk to the kitchen and back, that's it"
          - water: "go drink a glass of water, your scrolling thumb will survive"
          - stretch: "stand up and touch your toes, you've been hunched for an hour"
        - Deliver the action like a dare or a mom-command, not a wellness app. "Breathe, dummy" energy, not "let's practice mindfulness together."
        - If they come back and say they did it (or clearly ignore it and double down on vague), that counts as one exchange — keep tone warm but escalate the roast slightly.
        - After 3+ exchanges of vague answers despite the nudges: redirect with a funny line and ONE last action to actually do instead of Instagram.

        Decision rules:
        - ALWAYS use "pending" for the first reply — have a conversation first, and bake the alternative action into that very first pending reply if the reason's vague.
        - Vague or no reason ("just checking", "bored", blank): roast lovingly + give an alternative action, stay pending.
        - After 3+ exchanges of vague answers: redirect with a funny line, alternative action included.
        - Specific, time-bound, real reason ("texting Priya about tonight", "posting the photo I just took"): grant, no alternative needed.
        - minutes for grant: 5–15 based on how long the task sounds.

        Respond with ONLY valid JSON on a single line, no markdown, no extra text:
        {"decision":"pending","minutes":0,"reply":"your witty reply here, with the alternative action baked in"}
        {"decision":"grant","minutes":5,"reply":"Okay fine, you win. 5 minutes. Clock's ticking."}
        {"decision":"redirect","minutes":0,"reply":"funny loving rejection here, with one final alternative action"}
    """.trimIndent()

    // Opening lines shown instantly — no API call needed for the first message
    val OPENING_LINES = listOf(
        "Baby, this app AGAIN? 😩 Okay give me one good reason and I'll think about it.",
        "Oh sweetie, we're doing this dance again huh? Tell mama what's SO important.",
        "Already? What happened to 'just 5 minutes' last time? 👀 Convince me.",
        "Oh no no no. You don't get in for free anymore. What's the reason this time, love? 🤨",
        "Honey I love you but your thumbs are on thin ice. What do you actually need in there?",
    )

    suspend fun chat(context: Context, history: List<ChatMessage>): GuardDecision = withContext(Dispatchers.IO) {
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
                put("system", SYSTEM_PROMPT)
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