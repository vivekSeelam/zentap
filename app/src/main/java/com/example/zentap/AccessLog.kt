package com.example.zentap

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class AccessLog(
    val timestamp: Long,
    val allowed: Boolean,
    val durationMinutes: Int,
    val reason: String
)

object AccessLogger {

    private const val FILE_NAME = "access_log.json"
    private const val MAX_ENTRIES = 200
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun log(context: Context, entry: AccessLog) {
        ioScope.launch {
            try {
                val file = File(context.applicationContext.filesDir, FILE_NAME)
                val list = readFile(file).toMutableList()
                list.add(entry)
                if (list.size > MAX_ENTRIES) list.subList(0, list.size - MAX_ENTRIES).clear()
                writeFile(file, list)
            } catch (_: Exception) {}
        }
    }

    fun readAll(context: Context): List<AccessLog> =
        readFile(File(context.applicationContext.filesDir, FILE_NAME))

    private fun readFile(file: File): List<AccessLog> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AccessLog(
                    timestamp       = o.getLong("ts"),
                    allowed         = o.getBoolean("allowed"),
                    durationMinutes = o.getInt("dur"),
                    reason          = o.getString("reason")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun writeFile(file: File, entries: List<AccessLog>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("ts",      e.timestamp)
                put("allowed", e.allowed)
                put("dur",     e.durationMinutes)
                put("reason",  e.reason)
            })
        }
        file.writeText(arr.toString())
    }
}
