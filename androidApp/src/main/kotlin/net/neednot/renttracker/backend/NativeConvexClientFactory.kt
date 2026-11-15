package net.neednot.renttracker.backend

import dev.convex.android.MobileConvexClientInterface
import dev.convex.android.WebSocketStateSubscriber

typealias NativeConvexClientFactory =
    (deploymentUrl: String, clientId: String, webSocketStateSubscriber: WebSocketStateSubscriber?) ->
    MobileConvexClientInterface
