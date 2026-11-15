package net.neednot.renttracker.core.data

sealed interface PendingSendOutcome {
    data object Accepted : PendingSendOutcome

    data class PossibleDuplicate(
        val matches: List<DuplicateMatchDetails>,
    ) : PendingSendOutcome

    data class Rejected(
        val reason: PendingRejectionReason,
        val message: String,
    ) : PendingSendOutcome

    data object RetryLater : PendingSendOutcome
}
