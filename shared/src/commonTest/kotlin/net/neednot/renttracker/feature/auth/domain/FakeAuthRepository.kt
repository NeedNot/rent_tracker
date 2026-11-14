package net.neednot.renttracker.feature.auth.domain

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.MutableStateFlow
import net.neednot.renttracker.core.auth.AuthSessionState

class FakeAuthRepository : AuthRepository {
    override val sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.SignedOut)

    var sendSignInCodeResult: Result<Unit, AuthError> = Ok(Unit)
    var signInResult: Result<SignedInAccount, AuthError> = Ok(SignedInAccount(displayName = "dalton.h", isNew = false))
    var setDisplayNameResult: Result<String, AuthError> = Ok("Dalton")

    val sentCodesTo = mutableListOf<String>()
    val codeSignIns = mutableListOf<Pair<String, String>>()
    val savedDisplayNames = mutableListOf<String>()

    override suspend fun sendSignInCode(email: String): Result<Unit, AuthError> {
        sentCodesTo += email
        return sendSignInCodeResult
    }

    override suspend fun signInWithCode(
        email: String,
        code: String,
    ): Result<SignedInAccount, AuthError> {
        codeSignIns += email to code
        return signInResult
    }

    override suspend fun signInWithGoogle(): Result<SignedInAccount, AuthError> = signInResult

    override suspend fun setDisplayName(displayName: String): Result<String, AuthError> {
        savedDisplayNames += displayName
        return setDisplayNameResult
    }

    override suspend fun signOut(): Result<Unit, AuthError> = Ok(Unit)
}
