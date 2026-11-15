package net.neednot.renttracker.di

import net.neednot.renttracker.BuildConfig
import net.neednot.renttracker.auth.CredentialManagerGoogleIdTokenProvider
import net.neednot.renttracker.auth.ForegroundActivity
import net.neednot.renttracker.backend.ConvexBackend
import net.neednot.renttracker.core.auth.AuthSession
import net.neednot.renttracker.core.auth.BetterAuthSession
import net.neednot.renttracker.core.auth.GoogleIdTokenProvider
import net.neednot.renttracker.core.backend.Backend
import net.neednot.renttracker.core.data.Connectivity
import net.neednot.renttracker.core.data.PendingPaymentSyncScheduler
import net.neednot.renttracker.core.telemetry.LogcatTelemetry
import net.neednot.renttracker.core.telemetry.NoOpTelemetry
import net.neednot.renttracker.core.telemetry.Telemetry
import net.neednot.renttracker.sync.AndroidConnectivity
import net.neednot.renttracker.sync.SyncProblemNotifier
import net.neednot.renttracker.sync.WorkManagerSyncScheduler
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidAppModule =
    module {
        single<AuthSession> { BetterAuthSession(context = androidContext(), authUrl = BuildConfig.AUTH_URL) }
        single<Backend> {
            ConvexBackend(
                deploymentUrl = BuildConfig.CONVEX_URL,
                context = androidContext(),
                authSession = get(),
                telemetry = get(),
            )
        }
        single(createdAtStart = true) { ForegroundActivity(androidApplication()) }
        single<GoogleIdTokenProvider> {
            CredentialManagerGoogleIdTokenProvider(
                webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
                foregroundActivity = get(),
            )
        }
        single<Connectivity> { AndroidConnectivity(androidContext()) }
        single<PendingPaymentSyncScheduler> { WorkManagerSyncScheduler(androidContext()) }
        single { SyncProblemNotifier(androidContext()) }
        single<Telemetry> { if (BuildConfig.DEBUG) LogcatTelemetry() else NoOpTelemetry() }
    }
