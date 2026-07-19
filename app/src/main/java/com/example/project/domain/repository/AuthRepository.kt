package com.example.project.domain.repository

/**
 * Remote authentication against the Melodify backend.
 * Successful login/register persist the session locally (DataStore).
 */
interface AuthRepository {
    suspend fun login(handle: String, password: String): Result<Unit>
    suspend fun register(displayName: String, handle: String, password: String): Result<Unit>
    suspend fun logout()
}
