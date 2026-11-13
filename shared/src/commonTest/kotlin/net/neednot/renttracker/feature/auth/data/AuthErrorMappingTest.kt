package net.neednot.renttracker.feature.auth.data

import net.neednot.renttracker.core.auth.AuthSessionError
import net.neednot.renttracker.core.auth.GoogleIdTokenError
import net.neednot.renttracker.core.backend.BackendError
import net.neednot.renttracker.feature.auth.domain.AuthError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthErrorMappingTest {
    @Test
    fun everySessionErrorBecomesItsAuthError() {
        val expected =
            mapOf(
                AuthSessionError.NotSignedIn to AuthError.SessionExpired,
                AuthSessionError.SessionExpired to AuthError.SessionExpired,
                AuthSessionError.InvalidCode to AuthError.InvalidCode,
                AuthSessionError.CodeExpired to AuthError.CodeExpired,
                AuthSessionError.TooManyAttempts to AuthError.TooManyAttempts,
                AuthSessionError.RateLimited(retryAfterSeconds = 42) to AuthError.RateLimited(retryAfterSeconds = 42),
                AuthSessionError.RateLimited(retryAfterSeconds = null) to
                    AuthError.RateLimited(retryAfterSeconds = null),
                AuthSessionError.Network to AuthError.Offline,
            )

        expected.forEach { (sessionError, authError) -> assertEquals(authError, sessionError.toAuthError()) }
    }

    @Test
    fun sessionErrorsWithNoUserFixAreUnexpected() {
        assertIs<AuthError.Unexpected>(AuthSessionError.Storage.toAuthError())
        assertIs<AuthError.Unexpected>(AuthSessionError.Server(500, "BOOM", "broken").toAuthError())
        assertIs<AuthError.Unexpected>(AuthSessionError.UnexpectedResponse("no cookie").toAuthError())
    }

    @Test
    fun serverRejectingTheGoogleTokenIsAGoogleSignInFailure() {
        val rejected = AuthSessionError.Server(status = 401, code = "INVALID_TOKEN", message = null)

        assertEquals(AuthError.GoogleSignInFailed, rejected.toGoogleAuthError())
        assertEquals(AuthError.Offline, AuthSessionError.Network.toGoogleAuthError())
    }

    @Test
    fun googleTokenErrorsTellCancellingFromFailing() {
        assertEquals(AuthError.GoogleSignInCancelled, GoogleIdTokenError.Cancelled.toAuthError())
        assertEquals(AuthError.GoogleSignInFailed, GoogleIdTokenError.Failed("no play services").toAuthError())
    }

    @Test
    fun backendErrorsMapByConvexCode() {
        assertEquals(AuthError.SessionExpired, BackendError.Convex("NotSignedIn", "Sign in", null).toAuthError())
        assertEquals(AuthError.InvalidDisplayName, BackendError.Convex("ValidationFailed", "Bad", null).toAuthError())
        assertIs<AuthError.Unexpected>(BackendError.Convex("NotFound", "Missing", null).toAuthError())
        assertIs<AuthError.Unexpected>(BackendError.Server("down").toAuthError())
        assertIs<AuthError.Unexpected>(BackendError.Internal("bug").toAuthError())
    }
}
