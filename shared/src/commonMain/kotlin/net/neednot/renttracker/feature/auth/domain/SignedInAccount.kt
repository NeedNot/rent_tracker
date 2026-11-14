package net.neednot.renttracker.feature.auth.domain

data class SignedInAccount(
    val displayName: String,
    val isNew: Boolean,
)
