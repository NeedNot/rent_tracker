package net.neednot.renttracker.core.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection

suspend fun <T> RentTrackerDatabase.inWriteTransaction(write: suspend () -> T): T =
    useWriterConnection { connection -> connection.immediateTransaction { write() } }
