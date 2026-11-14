package net.neednot.renttracker.app

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import net.neednot.renttracker.core.auth.AuthSession
import net.neednot.renttracker.core.auth.GoogleIdTokenProvider
import net.neednot.renttracker.core.auth.NotYetSupportedAuthSession
import net.neednot.renttracker.core.auth.NotYetSupportedGoogleIdTokenProvider
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.backend.NotYetSupportedBackend
import net.neednot.renttracker.core.data.RentTrackerDatabase
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.core.telemetry.Telemetry
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

val iosPlatformModule =
    module {
        single<Telemetry> { NoOpTelemetry() }
        single<AuthSession> { NotYetSupportedAuthSession() }
        single<GoogleIdTokenProvider> { NotYetSupportedGoogleIdTokenProvider() }
        single<Backend> { NotYetSupportedBackend() }
        single<RoomDatabase.Builder<RentTrackerDatabase>> {
            Room.databaseBuilder<RentTrackerDatabase>(
                name = "${documentDirectory()}/${RentTrackerDatabase.FILE_NAME}",
            )
        }
    }

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val directory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return requireNotNull(directory?.path) { "No document directory" }
}
