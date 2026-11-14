package net.neednot.renttracker.auth

import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import net.neednot.renttracker.core.auth.GoogleIdToken
import net.neednot.renttracker.core.auth.GoogleIdTokenError
import net.neednot.renttracker.core.auth.GoogleIdTokenProvider
import java.security.SecureRandom

class CredentialManagerGoogleIdTokenProvider(
    private val webClientId: String,
    private val foregroundActivity: ForegroundActivity,
) : GoogleIdTokenProvider {
    init {
        require(webClientId.isNotBlank()) { "google.webClientId is missing from local.properties" }
    }

    override suspend fun requestIdToken(): Result<GoogleIdToken, GoogleIdTokenError> {
        val activity =
            foregroundActivity.current
                ?: return Err(GoogleIdTokenError.Failed("No screen is open to show Google sign-in"))
        val nonce = newNonce()
        val request =
            GetCredentialRequest
                .Builder()
                .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).setNonce(nonce).build())
                .build()
        return try {
            CredentialManager.create(activity).getCredential(activity, request).toGoogleIdToken(nonce)
        } catch (_: GetCredentialCancellationException) {
            Err(GoogleIdTokenError.Cancelled)
        } catch (error: GetCredentialException) {
            Err(GoogleIdTokenError.Failed("${error.type}: ${error.message}"))
        }
    }

    private fun GetCredentialResponse.toGoogleIdToken(nonce: String): Result<GoogleIdToken, GoogleIdTokenError> {
        val credential = credential
        val isGoogleIdToken =
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        if (!isGoogleIdToken) return Err(GoogleIdTokenError.Failed("Unexpected credential type ${credential.type}"))
        return try {
            Ok(GoogleIdToken(idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken, nonce = nonce))
        } catch (error: GoogleIdTokenParsingException) {
            Err(GoogleIdTokenError.Failed("Unreadable Google ID token: ${error.message}"))
        }
    }

    private fun newNonce(): String {
        val bytes = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private companion object {
        const val NONCE_BYTES = 32
    }
}
