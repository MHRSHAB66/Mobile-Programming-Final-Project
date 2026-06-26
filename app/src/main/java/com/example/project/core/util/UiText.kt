package com.example.project.core.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * A text value that may come from a string resource or a raw string. Lets non-UI layers
 * (repositories, use cases, ViewModels) describe user-facing text via resource ids instead
 * of hardcoding strings, satisfying the "no user-facing text in Kotlin" requirement.
 */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText
    class Resource(@StringRes val resId: Int, vararg val args: Any) : UiText

    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(resId, *args)
    }

    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> LocalContext.current.getString(resId, *args)
    }

    companion object {
        fun from(@StringRes resId: Int, vararg args: Any): UiText = Resource(resId, *args)
        fun raw(value: String): UiText = Dynamic(value)
    }
}
