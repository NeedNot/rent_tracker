package net.neednot.renttracker.core.auth

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthSession : AuthSession {
    override val state = MutableStateFlow<AuthSessionState>(AuthSessionState.SignedOut)

    var sendSignInCodeResult: Result<Unit, AuthSessionError> = Ok(Unit)
    var signInResult: Result<Unit, AuthSessionError> = Ok(Unit)
    var signOutResult: Result<Unit, AuthSessionError> = Ok(Unit)

    val sentCodesTo = mutableListOf<String>()
    val codeSignIns = mutableListOf<Pair<String, String>>()
    val googleSignIns = mutableListOf<GoogleIdToken>()
    var signOutCount = 0

    override suspend fun sendSignInCode(email: String): Result<Unit, AuthSessionError> {
        sentCodesTo += email
        return sendSignInCodeResult
    }

    override suspend fun signInWithCode(
        email: String,
        code: String,
    ): Result<Unit, AuthSessionError> {
        codeSignIns += email to code
        return signInResult.onOk { state.value = AuthSessionState.SignedIn(USER_ID) }
    }

    override suspend fun signInWithGoogle(token: GoogleIdToken): Result<Unit, AuthSessionError> {
        googleSignIns += token
        return signInResult.onOk { state.value = AuthSessionState.SignedIn(USER_ID) }
    }

    override suspend fun refreshJwt(): Result<String, AuthSessionError> = Ok("jwt")

    override suspend fun signOut(): Result<Unit, AuthSessionError> {
        signOutCount++
        state.value = AuthSessionState.SignedOut
        return signOutResult
    }

    companion object {
        const val USER_ID = "user-1"
    }
}
