package net.neednot.renttracker.feature.tenants.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.designsystem.components.Banner
import net.neednot.renttracker.core.designsystem.components.BannerKind
import net.neednot.renttracker.core.designsystem.components.ConfirmDialog
import net.neednot.renttracker.core.designsystem.components.ContentLayout
import net.neednot.renttracker.core.designsystem.components.ErrorState
import net.neednot.renttracker.core.designsystem.components.LoadingIndicator
import net.neednot.renttracker.core.designsystem.components.OfflineBanner
import net.neednot.renttracker.core.designsystem.components.RtTopAppBar
import net.neednot.renttracker.core.designsystem.components.StatusChip
import net.neednot.renttracker.core.designsystem.components.StatusChipKind
import net.neednot.renttracker.core.designsystem.components.UnimplementedAction
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.designsystem.theme.Spacing
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.components.BalanceCard
import net.neednot.renttracker.feature.tenants.presentation.components.CalendarActions
import net.neednot.renttracker.feature.tenants.presentation.components.CalendarCard
import net.neednot.renttracker.feature.tenants.presentation.components.LateFeeSnackbar
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerEntryHost
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerItemActions
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerPreviewCard
import net.neednot.renttracker.feature.tenants.presentation.components.MonthDetailHost
import net.neednot.renttracker.feature.tenants.presentation.components.PendingBanner
import net.neednot.renttracker.feature.tenants.presentation.components.PendingPaymentHost
import net.neednot.renttracker.feature.tenants.presentation.components.RentTermsCard
import net.neednot.renttracker.feature.tenants.presentation.components.TenantDetailsCard
import net.neednot.renttracker.feature.tenants.presentation.components.TenantMenu
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryEditTarget
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantBalanceUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantConfirmationUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantDetailsUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantUiState
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.TenantViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.ic_inventory_2
import net.neednot.renttracker.resources.tenant_archive_confirm
import net.neednot.renttracker.resources.tenant_archive_title
import net.neednot.renttracker.resources.tenant_archived_label
import net.neednot.renttracker.resources.tenant_error_title
import net.neednot.renttracker.resources.tenant_restore_confirm
import net.neednot.renttracker.resources.tenant_restore_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TenantScreen(
    route: TenantRoute,
    onBackClicked: () -> Unit,
    onEditDetailsClicked: (groupId: String) -> Unit,
    onChangeRentClicked: () -> Unit,
    onRentTermClicked: (rentTermId: String) -> Unit,
    onLogPaymentClicked: () -> Unit,
    onAddChargeClicked: (suggestedMonth: String?, suggestedCents: Long?) -> Unit,
    onViewLedgerClicked: () -> Unit,
    onEditEntryClicked: (EntryEditTarget) -> Unit,
    onMoveClicked: () -> Unit,
    onEditPendingPaymentClicked: (clientId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TenantViewModel = koinViewModel { parametersOf(route) },
) {
    val uiState by viewModel.uiState.collectAsState()
    PendingPaymentHost(
        route = route,
        onEditClicked = onEditPendingPaymentClicked,
    ) { onPendingClicked ->
        LedgerEntryHost(
            tenantId = route.tenantId,
            onEditClicked = onEditEntryClicked,
            content = { onLedgerRowClicked, onLedgerRestoreClicked ->
                TenantContent(
                    uiState = uiState,
                    actions =
                        TenantActions(
                            onBackClicked = onBackClicked,
                            onEditDetailsClicked = onEditDetailsClicked,
                            onChangeRentClicked = onChangeRentClicked,
                            onRentTermClicked = onRentTermClicked,
                            onLogPaymentClicked = onLogPaymentClicked,
                            onAddChargeClicked = { onAddChargeClicked(null, null) },
                            onViewLedgerClicked = onViewLedgerClicked,
                            onLateFeeAddClicked = { suggestion ->
                                onAddChargeClicked(suggestion.month, suggestion.cents)
                            },
                            onLateFeeDismissed = viewModel::onLateFeeDismissed,
                            onMoveClicked = onMoveClicked,
                            onArchiveClicked = viewModel::onArchiveClicked,
                            onRestoreClicked = viewModel::onRestoreClicked,
                            onStatusChangeConfirmed = viewModel::onStatusChangeConfirmed,
                            onStatusChangeDismissed = viewModel::onStatusChangeDismissed,
                        ),
                    itemActions = LedgerItemActions(onLedgerRowClicked, onLedgerRestoreClicked, onPendingClicked),
                    calendarActions =
                        CalendarActions(
                            onPreviousYearClicked = viewModel::onPreviousYearClicked,
                            onNextYearClicked = viewModel::onNextYearClicked,
                            onMonthClicked = viewModel::onMonthClicked,
                            onMonthDetailDismissed = viewModel::onMonthDetailDismissed,
                            onAddFeeClicked = { addFee ->
                                viewModel.onMonthDetailDismissed()
                                onAddChargeClicked(addFee.month, addFee.suggestedCents)
                            },
                        ),
                    modifier = modifier,
                )
            },
        )
    }
}

@Composable
internal fun TenantContent(
    uiState: TenantUiState,
    actions: TenantActions,
    modifier: Modifier = Modifier,
    itemActions: LedgerItemActions = LedgerItemActions.None,
    calendarActions: CalendarActions = CalendarActions.None,
) {
    val content = uiState as? TenantUiState.Content
    Scaffold(
        modifier = modifier,
        topBar = { TenantTopBar(uiState, actions) },
        bottomBar = {
            content?.lateFeeSuggestion?.let { suggestion ->
                LateFeeSnackbar(
                    suggestion = suggestion,
                    onDismissClicked = actions.onLateFeeDismissed,
                    onAddClicked = { actions.onLateFeeAddClicked(suggestion) },
                )
            }
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            content?.offlineBanner?.let { OfflineBanner(text = it.asString()) }
            when (uiState) {
                TenantUiState.Loading -> {
                    LoadingIndicator()
                }

                is TenantUiState.Error -> {
                    ErrorState(
                        title = stringResource(Res.string.tenant_error_title),
                        text = uiState.message.asString(),
                        onRetryClicked = UnimplementedAction,
                        modifier = Modifier.padding(ContentLayout.List.padding),
                    )
                }

                is TenantUiState.Content -> {
                    TenantSections(uiState, actions, itemActions, calendarActions)
                }
            }
        }
    }
    content?.monthDetail?.let { detail -> MonthDetailHost(detail = detail, actions = calendarActions) }
    content?.statusChange?.confirmation?.let { confirmation ->
        StatusChangeDialog(
            confirmation = confirmation,
            isWorking = content.statusChange.isSaving,
            onConfirmed = actions.onStatusChangeConfirmed,
            onDismissed = actions.onStatusChangeDismissed,
        )
    }
}

@Composable
private fun TenantTopBar(
    uiState: TenantUiState,
    actions: TenantActions,
) {
    val content = uiState as? TenantUiState.Content
    RtTopAppBar(
        title = content?.name.orEmpty(),
        subtitle = content?.groupName,
        onNavigationClicked = actions.onBackClicked,
        actions = {
            if (content?.isArchived == true) {
                StatusChip(
                    text = stringResource(Res.string.tenant_archived_label),
                    kind = StatusChipKind.Neutral,
                    modifier = Modifier.padding(end = Spacing.xs),
                    icon = Res.drawable.ic_inventory_2,
                )
            }
            content?.takeIf { it.hasMenu }?.let {
                TenantMenu(
                    isArchived = it.isArchived,
                    canMove = it.canMove,
                    onEditDetailsClicked = { actions.onEditDetailsClicked(it.groupId) },
                    onMoveClicked = actions.onMoveClicked,
                    onArchiveClicked = actions.onArchiveClicked,
                    onRestoreClicked = actions.onRestoreClicked,
                )
            }
        },
    )
}

@Composable
private fun TenantSections(
    tenant: TenantUiState.Content,
    actions: TenantActions,
    itemActions: LedgerItemActions,
    calendarActions: CalendarActions,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(ContentLayout.List.padding),
        verticalArrangement = Arrangement.spacedBy(ContentLayout.List.spacing),
    ) {
        tenant.pendingBanner?.let { banner ->
            PendingBanner(banner = banner, onReviewClicked = itemActions.onPendingClicked)
        }
        listOfNotNull(tenant.syncError, tenant.statusChange.error).forEach { error ->
            Banner(text = error.asString(), kind = BannerKind.Error)
        }
        BalanceCard(
            balance = tenant.balance,
            canLogPayment = tenant.canLogPayment,
            canAddCharge = tenant.canAddCharge,
            onLogPaymentClicked = actions.onLogPaymentClicked,
            onAddChargeClicked = actions.onAddChargeClicked,
        )
        tenant.calendar?.let { calendar -> CalendarCard(calendar = calendar, actions = calendarActions) }
        LedgerPreviewCard(
            tenantName = tenant.name,
            items = tenant.ledgerPreview,
            actions = itemActions,
            onViewFullLedgerClicked = actions.onViewLedgerClicked,
        )
        RentTermsCard(
            rentTerms = tenant.rentTerms,
            isEditable = tenant.canEditDetails,
            onChangeRentClicked = actions.onChangeRentClicked,
            onRentTermClicked = actions.onRentTermClicked,
        )
        TenantDetailsCard(
            details = tenant.details,
            isEditable = tenant.canEditDetails,
            onEditClicked = { actions.onEditDetailsClicked(tenant.groupId) },
        )
    }
}

@Composable
private fun StatusChangeDialog(
    confirmation: TenantConfirmationUiState,
    isWorking: Boolean,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    val (title, confirmLabel) =
        when (confirmation.action) {
            TenantStatusAction.Archive -> Res.string.tenant_archive_title to Res.string.tenant_archive_confirm
            TenantStatusAction.Restore -> Res.string.tenant_restore_title to Res.string.tenant_restore_confirm
        }
    ConfirmDialog(
        title = stringResource(title),
        text = confirmation.message.asString(),
        confirmLabel = stringResource(confirmLabel),
        onConfirmed = onConfirmed,
        onDismissed = onDismissed,
        isConfirmEnabled = !isWorking,
    )
}

@Preview
@Composable
private fun TenantContentPreview() {
    RentTrackerTheme {
        TenantContent(
            uiState =
                TenantUiState.Content(
                    name = "Apt 3B",
                    groupId = "g1",
                    groupName = "1st Street",
                    isArchived = false,
                    balance =
                        TenantBalanceUiState(
                            state = TenantBalanceState.Late,
                            total = UiText.Plain("Owes $550.00"),
                            lines = emptyList(),
                            nextDue = null,
                            partialMoveOutWarning = null,
                        ),
                    rentTerms =
                        listOf(
                            RentTermRowUiState(
                                rentTermId = "r2",
                                summary = UiText.Plain("$1,500.00 a month · due the 1st"),
                                period = UiText.Plain("From Jan 2025"),
                                isCurrent = true,
                            ),
                        ),
                    details =
                        TenantDetailsUiState(
                            contactName = "Dana Whitfield",
                            phone = null,
                            email = null,
                            movedIn = "Jan 1, 2024",
                            lastDayInUnit = null,
                            securityDeposit = null,
                            gracePeriod = UiText.Plain("5 days (group)"),
                            lateFee = UiText.Plain("$50.00 (group)"),
                            notes = null,
                        ),
                ),
            actions = TenantActions(),
        )
    }
}
