package net.neednot.renttracker.core.auth

import com.github.michaelbull.result.Result

interface GoogleIdTokenProvider {
    suspend fun requestIdToken(): Result<GoogleIdToken, GoogleIdTokenError>
}
