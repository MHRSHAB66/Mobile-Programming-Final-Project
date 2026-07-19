package com.example.project.data.remote.api

import com.example.project.R
import com.example.project.core.util.UiText
import retrofit2.HttpException
import java.io.IOException

sealed class AuthException(val uiText: UiText) : Exception() {
    class InvalidCredentials : AuthException(UiText.from(R.string.auth_error_invalid_credentials))
    class HandleTaken : AuthException(UiText.from(R.string.auth_error_handle_taken))
    class Network : AuthException(UiText.from(R.string.auth_error_network))
    class Validation : AuthException(UiText.from(R.string.auth_error_validation))
    class Unknown : AuthException(UiText.from(R.string.auth_error_unknown))
}

fun Throwable.toAuthException(): AuthException {
    if (this is AuthException) return this
    return when (this) {
        is IOException -> AuthException.Network()
        is HttpException -> when (code()) {
            401 -> AuthException.InvalidCredentials()
            409 -> AuthException.HandleTaken()
            422 -> AuthException.Validation()
            else -> AuthException.Unknown()
        }
        else -> AuthException.Unknown()
    }
}
