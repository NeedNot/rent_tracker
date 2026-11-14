package net.neednot.renttracker.backend

import android.content.Context
import com.github.michaelbull.result.fold
import dev.convex.android.AuthProvider
import net.neednot.renttracker.core.auth.AuthSession
import net.neednot.renttracker.core.auth.AuthSessionError

class SessionJwtProvider(
    private val authSession: AuthSession,
) : AuthProvider<String> {
    override suspend fun login(
        context: Context,
        onIdToken: (String?) -> Unit,
    ): Result<String> = loginFromCache(onIdToken)

    // Convex calls this again whenever its connection needs a new token, so every call asks the auth server.
    override suspend fun loginFromCache(onIdToken: (String?) -> Unit): Result<String> =
        authSession.refreshJwt().fold(
            success = { jwt -> Result.success(jwt) },
            failure = { error ->
                onIdToken(null)
                Result.failure(JwtUnavailable(error))
            },
        )

    override suspend fun logout(context: Context): Result<Void?> = Result.success(null)

    override fun extractIdToken(authResult: String): String = authResult

    class JwtUnavailable(
        val error: AuthSessionError,
    ) : Exception("No JWT for Convex: $error")
}
