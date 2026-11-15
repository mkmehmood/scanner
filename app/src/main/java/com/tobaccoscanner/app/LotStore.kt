package com.tobaccoscanner.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Lot(
    val id: Long,
    val name: String,
    val timestamp: String,
    val leaf: Boolean,
    val grade: String,
    val score: Int,
    val pctOptimal: Int,
    val pctPale: Int,
    val handChecks: Int,
)

/** Saved checks (the 20 newest) kept in private app storage. */
object LotStore {
    private const val FILE = "lots"
    private const val KEY = "list"
    private const val MAX = 20

    fun load(ctx: Context): List<Lot> {
        val raw = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Lot(
                    id = o.getLong("id"),
                    name = o.optString("name"),
                    timestamp = o.optString("timestamp"),
                    leaf = o.optBoolean("leaf", true),
                    grade = o.optString("grade", "B"),
                    score = o.optInt("score"),
                    pctOptimal = o.optInt("pctOptimal"),
                    pctPale = o.optInt("pctPale"),
                    handChecks = o.optInt("handChecks"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(ctx: Context, lots: List<Lot>): List<Lot> {
        val trimmed = lots.take(MAX)
        val arr = JSONArray()
        trimmed.forEach { l ->
            arr.put(
                JSONObject()
                    .put("id", l.id).put("name", l.name).put("timestamp", l.timestamp)
                    .put("leaf", l.leaf).put("grade", l.grade).put("score", l.score)
                    .put("pctOptimal", l.pctOptimal).put("pctPale", l.pctPale).put("handChecks", l.handChecks),
            )
        }
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
        return trimmed
    }
}
