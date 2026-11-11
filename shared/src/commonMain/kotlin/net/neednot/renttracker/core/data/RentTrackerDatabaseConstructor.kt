package net.neednot.renttracker.core.data

import androidx.room.RoomDatabaseConstructor

// Room's compiler generates the actual implementation for every target.
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object RentTrackerDatabaseConstructor : RoomDatabaseConstructor<RentTrackerDatabase> {
    override fun initialize(): RentTrackerDatabase
}
