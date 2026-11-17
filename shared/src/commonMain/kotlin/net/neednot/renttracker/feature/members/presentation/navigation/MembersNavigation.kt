package net.neednot.renttracker.feature.members.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import net.neednot.renttracker.core.navigation.InviteMemberRoute
import net.neednot.renttracker.core.navigation.MemberPermissionsRoute
import net.neednot.renttracker.core.navigation.MembersRoute
import net.neednot.renttracker.core.navigation.TransferOwnershipRoute
import net.neednot.renttracker.feature.members.presentation.screens.InviteMemberScreen
import net.neednot.renttracker.feature.members.presentation.screens.MemberPermissionsScreen
import net.neednot.renttracker.feature.members.presentation.screens.MembersScreen
import net.neednot.renttracker.feature.members.presentation.screens.TransferOwnershipScreen

fun NavGraphBuilder.membersGraph(navController: NavController) {
    composable<MembersRoute> { entry ->
        val groupId = entry.toRoute<MembersRoute>().groupId
        MembersScreen(
            groupId = groupId,
            onBackClicked = { navController.popBackStack() },
            onInviteClicked = { navController.navigate(InviteMemberRoute(groupId)) },
            onMemberClicked = { membershipId -> navController.navigate(MemberPermissionsRoute(groupId, membershipId)) },
            onTransferOwnershipClicked = { navController.navigate(TransferOwnershipRoute(groupId)) },
        )
    }
    composable<InviteMemberRoute> { entry ->
        InviteMemberScreen(
            groupId = entry.toRoute<InviteMemberRoute>().groupId,
            onClosed = { navController.popBackStack() },
        )
    }
    composable<MemberPermissionsRoute> { entry ->
        val route = entry.toRoute<MemberPermissionsRoute>()
        MemberPermissionsScreen(
            groupId = route.groupId,
            membershipId = route.membershipId,
            onClosed = { navController.popBackStack() },
        )
    }
    composable<TransferOwnershipRoute> { entry ->
        val groupId = entry.toRoute<TransferOwnershipRoute>().groupId
        TransferOwnershipScreen(
            groupId = groupId,
            onInviteClicked = { navController.navigate(InviteMemberRoute(groupId)) },
            onClosed = { navController.popBackStack() },
        )
    }
}
