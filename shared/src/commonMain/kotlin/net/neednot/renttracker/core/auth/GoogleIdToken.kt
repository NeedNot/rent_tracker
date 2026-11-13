package net.neednot.renttracker.core.auth

data class GoogleIdToken(
    val idToken: String,
    val nonce: String,
)
