package com.example.project.core.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Applies the selected app language by wrapping the base context with a localized
 * Configuration. Setting the locale also flips the layout direction, so Persian renders
 * RTL and English LTR automatically (Compose reads layout direction from this configuration).
 */
object LocaleManager {
    fun wrap(context: Context, languageTag: String): Context {
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
