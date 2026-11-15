package net.neednot.renttracker.feature.group.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import net.neednot.renttracker.core.navigation.ArchivedTenantsRoute
import net.neednot.renttracker.core.navigation.GroupRoute
import net.neednot.renttracker.core.navigation.GroupSettingsRoute
import net.neednot.renttracker.core.navigation.HomeRoute
import net.neednot.renttracker.core.navigation.MembersRoute
import net.neednot.renttracker.core.navigation.TenantEditorRoute
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.core.navigation.TransferOwnershipRoute
import net.neednot.renttracker.feature.group.presentation.screens.ArchivedTenantsScreen
import net.neednot.renttracker.feature.group.presentation.screens.GroupScreen
import net.neednot.renttracker.feature.group.presentation.screens.GroupSettingsScreen

fun NavGraphBuilder.groupGraph(navController: NavController) {
    composable<GroupRoute> { entry ->
        val groupId = entry.toRoute<GroupRoute>().groupId
        GroupScreen(
            groupId = groupId,
            onBackClicked = { navController.popBackStack() },
            onTenantClicked = { tenantId -> navController.navigate(TenantRoute(tenantId)) },
            onAddTenantClicked = { navController.navigate(TenantEditorRoute(groupId)) },
            onArchivedClicked = { navController.navigate(ArchivedTenantsRoute(groupId)) },
            onMembersClicked = { navController.navigate(MembersRoute(groupId)) },
            onSettingsClicked = { navController.navigate(GroupSettingsRoute(groupId)) },
            onTransferOwnershipClicked = { navController.navigate(TransferOwnershipRoute(groupId)) },
            onReturnHome = { navController.popBackStack<HomeRoute>(inclusive = false) },
        )
    }
    composable<GroupSettingsRoute> { entry ->
        GroupSettingsScreen(
            groupId = entry.toRoute<GroupSettingsRoute>().groupId,
            onClosed = { navController.popBackStack() },
        )
    }
    composable<ArchivedTenantsRoute> { entry ->
        ArchivedTenantsScreen(
            groupId = entry.toRoute<ArchivedTenantsRoute>().groupId,
            onBackClicked = { navController.popBackStack() },
            onTenantClicked = { tenantId -> navController.navigate(TenantRoute(tenantId)) },
        )
    }
}
