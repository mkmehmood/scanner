package com.tobaccoscanner.app

import android.content.Context
import java.util.Locale

/** Keeps the user's API key, model name and language choice on this phone only. */
object Prefs {
    const val DEFAULT_MODEL = "claude-sonnet-5-5"
    private const val FILE = "settings"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun key(ctx: Context): String = sp(ctx).getString("api_key", "") ?: ""
    fun model(ctx: Context): String =
        (sp(ctx).getString("model", DEFAULT_MODEL) ?: DEFAULT_MODEL).ifBlank { DEFAULT_MODEL }

    fun saveApi(ctx: Context, key: String, model: String) {
        sp(ctx).edit()
            .putString("api_key", key.trim())
            .putString("model", model.trim().ifBlank { DEFAULT_MODEL })
            .apply()
    }

    /** "ur" or "en". Defaults to the phone's language (Urdu if the phone is set to Urdu, otherwise English). */
    fun lang(ctx: Context): String {
        val saved = sp(ctx).getString("lang", "") ?: ""
        if (saved == "ur" || saved == "en") return saved
        return if (Locale.getDefault().language == "ur") "ur" else "en"
    }

    fun setLang(ctx: Context, lang: String) {
        sp(ctx).edit().putString("lang", lang).apply()
    }
}
