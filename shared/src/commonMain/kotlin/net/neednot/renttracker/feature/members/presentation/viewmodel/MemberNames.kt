package net.neednot.renttracker.feature.members.presentation.viewmodel

private val WORD_SEPARATOR = Regex("\\s+")
private const val INITIALS_LENGTH = 2

internal fun initialsOf(displayName: String): String =
    words(displayName).take(INITIALS_LENGTH).joinToString(separator = "") { it.first().uppercase() }

internal fun firstNameOf(displayName: String): String = words(displayName).firstOrNull() ?: displayName

private fun words(displayName: String): List<String> =
    displayName.trim().split(WORD_SEPARATOR).filter {
        it.isNotEmpty()
    }
