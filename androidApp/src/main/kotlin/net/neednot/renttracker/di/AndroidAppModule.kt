package net.neednot.renttracker.di

import net.neednot.renttracker.BuildConfig
import net.neednot.renttracker.backend.ConvexBackend
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.telemetry.LogcatTelemetry
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.core.telemetry.Telemetry
import org.koin.dsl.module

val androidAppModule =
    module {
        single<Backend> { ConvexBackend(deploymentUrl = BuildConfig.CONVEX_URL) }
        single<Telemetry> { if (BuildConfig.DEBUG) LogcatTelemetry() else NoOpTelemetry() }
    }
