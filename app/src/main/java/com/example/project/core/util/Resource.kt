package com.example.project.core.util

/**
 * Generic wrapper for asynchronous results so screens can render loading / success / error
 * states without leaking exceptions into the UI layer.
 */
sealed interface Resource<out T> {
    data object Loading : Resource<Nothing>
    data class Success<T>(val data: T) : Resource<T>
    data class Error(val message: UiText) : Resource<Nothing>
}
