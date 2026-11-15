package net.neednot.renttracker.core.data

sealed interface PendingQueueError {
    data object TenantNotCached : PendingQueueError

    data object NotFound : PendingQueueError

    data object NotEditable : PendingQueueError
}
