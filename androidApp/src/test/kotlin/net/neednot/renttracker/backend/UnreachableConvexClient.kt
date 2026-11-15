package net.neednot.renttracker.backend

import dev.convex.android.AuthTokenProvider
import dev.convex.android.ClientException
import dev.convex.android.MobileConvexClientInterface
import dev.convex.android.QuerySubscriber
import dev.convex.android.SubscriptionHandle
import dev.convex.android.testing.FakeFfiClient

class UnreachableConvexClient(
    private val reachable: FakeFfiClient = FakeFfiClient(),
) : MobileConvexClientInterface by reachable {
    @Volatile
    var isReachable = false

    val subscriptions get() = reachable.subscriptions

    override suspend fun subscribe(
        name: String,
        args: Map<String, String>,
        subscriber: QuerySubscriber,
    ): SubscriptionHandle {
        failWhileUnreachable()
        return reachable.subscribe(name, args, subscriber)
    }

    override suspend fun setAuthCallback(provider: AuthTokenProvider?) {
        failWhileUnreachable()
        reachable.setAuthCallback(provider)
    }

    private fun failWhileUnreachable() {
        if (!isReachable) throw ClientException.InternalException("failed to connect to the deployment")
    }
}
