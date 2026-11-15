package com.tobaccoscanner.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Root() }
    }
}

/** A context that serves strings in the in-app language while still unwrapping to the Activity. */
private class LocalizedContext(base: Context, private val res: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = res
}

@Composable
private fun WithLanguage(lang: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val locale = remember(lang) { Locale.forLanguageTag(lang) }
    val config = remember(base, lang) {
        Configuration(base.resources.configuration).also {
            it.setLocale(locale)
            it.setLayoutDirection(locale)
        }
    }
    val localized = remember(base, config) { LocalizedContext(base, base.createConfigurationContext(config).resources) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides config,
        LocalLayoutDirection provides if (lang == "ur") LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) { content() }
}

@Composable
private fun Root() {
    val host = LocalContext.current
    var lang by remember { mutableStateOf(Prefs.lang(host)) }
    WithLanguage(lang) {
        TobaccoTheme {
            ScannerApp(lang = lang, onToggleLang = {
                lang = if (lang == "ur") "en" else "ur"
                Prefs.setLang(host, lang)
            })
        }
    }
}
