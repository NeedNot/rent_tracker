package net.neednot.renttracker.feature.tenants.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.neednot.renttracker.core.data.TenantBalanceState
import net.neednot.renttracker.core.designsystem.theme.RentTrackerTheme
import net.neednot.renttracker.core.navigation.TenantRoute
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.asString
import net.neednot.renttracker.feature.tenants.presentation.components.BalanceCard
import net.neednot.renttracker.feature.tenants.presentation.components.CalendarActions
import net.neednot.renttracker.feature.tenants.presentation.components.CalendarCard
import net.neednot.renttracker.feature.tenants.presentation.components.EntryActionButtons
import net.neednot.renttracker.feature.tenants.presentation.components.LateFeeSnackbar
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerEntryHost
import net.neednot.renttracker.feature.tenants.presentation.components.LedgerPreviewCard
import net.neednot.renttracker.feature.tenants.presentation.components.MonthDetailHost
import net.neednot.renttracker.feature.tenants.presentation.components.RentTermsCard
import net.neednot.renttracker.feature.tenants.presentation.components.TenantDetailsCard
import net.neednot.renttracker.feature.tenants.presentation.uistate.EntryEditTarget
import net.neednot.renttracker.feature.tenants.presentation.uistate.LateFeeSuggestionUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.RentTermRowUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantBalanceUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantConfirmationUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantDetailsUiState
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantStatusAction
import net.neednot.renttracker.feature.tenants.presentation.uistate.TenantUiState
import net.neednot.renttracker.feature.tenants.presentation.viewmodel.TenantViewModel
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.common_cancel
import net.neednot.renttracker.resources.tenant_archive_confirm
import net.neednot.renttracker.resources.tenant_archive_title
import net.neednot.renttracker.resources.tenant_archived_label
import net.neednot.renttracker.resources.tenant_back
import net.neednot.renttracker.resources.tenant_loading
import net.neednot.renttracker.resources.tenant_menu_archive
import net.neednot.renttracker.resources.tenant_menu_edit_details
import net.neednot.renttracker.resources.tenant_menu_restore
import net.neednot.renttracker.resources.tenant_more_options
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
    modifier: Modifier = Modifier,
    viewModel: TenantViewModel = koinViewModel { parametersOf(route) },
) {
    val uiState by viewModel.uiState.collectAsState()
    LedgerEntryHost(
        tenantId = route.tenantId,
        onEditClicked = onEditEntryClicked,
        content = { onLedgerRowClicked, onLedgerRestoreClicked ->
            TenantContent(
                uiState = uiState,
                onBackClicked = onBackClicked,
                onEditDetailsClicked = onEditDetailsClicked,
                onChangeRentClicked = onChangeRentClicked,
                onRentTermClicked = onRentTermClicked,
                onLogPaymentClicked = onLogPaymentClicked,
                onAddChargeClicked = { onAddChargeClicked(null, null) },
                onViewLedgerClicked = onViewLedgerClicked,
                onLedgerRowClicked = onLedgerRowClicked,
                onLedgerRestoreClicked = onLedgerRestoreClicked,
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
                onLateFeeAddClicked = { suggestion -> onAddChargeClicked(suggestion.month, suggestion.cents) },
                onLateFeeDismissed = viewModel::onLateFeeDismissed,
                onArchiveClicked = viewModel::onArchiveClicked,
                onRestoreClicked = viewModel::onRestoreClicked,
                onStatusChangeConfirmed = viewModel::onStatusChangeConfirmed,
                onStatusChangeDismissed = viewModel::onStatusChangeDismissed,
                modifier = modifier,
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TenantContent(
    uiState: TenantUiState,
    onBackClicked: () -> Unit,
    onEditDetailsClicked: (groupId: String) -> Unit,
    onChangeRentClicked: () -> Unit,
    onRentTermClicked: (rentTermId: String) -> Unit,
    onLogPaymentClicked: () -> Unit,
    onAddChargeClicked: () -> Unit,
    onViewLedgerClicked: () -> Unit,
    onLedgerRowClicked: (key: String) -> Unit,
    onLedgerRestoreClicked: (key: String) -> Unit,
    calendarActions: CalendarActions,
    onLateFeeAddClicked: (LateFeeSuggestionUiState) -> Unit,
    onLateFeeDismissed: () -> Unit,
    onArchiveClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onStatusChangeConfirmed: () -> Unit,
    onStatusChangeDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = uiState as? TenantUiState.Content
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { content?.let { TenantTitle(name = it.name, groupName = it.groupName) } },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.tenant_back),
                        )
                    }
                },
                actions = {
                    content?.let {
                        TenantMenu(
                            isArchived = it.isArchived,
                            onEditDetailsClicked = { onEditDetailsClicked(it.groupId) },
                            onArchiveClicked = onArchiveClicked,
                            onRestoreClicked = onRestoreClicked,
                        )
                    }
                },
            )
        },
        bottomBar = {
            content?.lateFeeSuggestion?.let { suggestion ->
                LateFeeSnackbar(
                    suggestion = suggestion,
                    onDismissClicked = onLateFeeDismissed,
                    onAddClicked = { onLateFeeAddClicked(suggestion) },
                )
            }
        },
    ) { contentPadding ->
        when (uiState) {
            TenantUiState.Loading -> {
                Loading(contentPadding)
            }

            is TenantUiState.Error -> {
                ErrorMessage(message = uiState.message, contentPadding = contentPadding)
            }

            is TenantUiState.Content -> {
                TenantSections(
                    tenant = uiState,
                    onEditDetailsClicked = { onEditDetailsClicked(uiState.groupId) },
                    onChangeRentClicked = onChangeRentClicked,
                    onRentTermClicked = onRentTermClicked,
                    onLogPaymentClicked = onLogPaymentClicked,
                    onAddChargeClicked = onAddChargeClicked,
                    onViewLedgerClicked = onViewLedgerClicked,
                    onLedgerRowClicked = onLedgerRowClicked,
                    onLedgerRestoreClicked = onLedgerRestoreClicked,
                    calendarActions = calendarActions,
                    contentPadding = contentPadding,
                )
            }
        }
    }
    content?.monthDetail?.let { detail ->
        MonthDetailHost(detail = detail, actions = calendarActions)
    }
    content?.statusChange?.confirmation?.let { confirmation ->
        StatusChangeDialog(
            confirmation = confirmation,
            onConfirmed = onStatusChangeConfirmed,
            onDismissed = onStatusChangeDismissed,
        )
    }
}

@Composable
private fun TenantTitle(
    name: String,
    groupName: String,
) {
    Column {
        Text(text = name, style = MaterialTheme.typography.titleLarge)
        Text(
            text = groupName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TenantMenu(
    isArchived: Boolean,
    onEditDetailsClicked: () -> Unit,
    onArchiveClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { isOpen = true }) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = stringResource(Res.string.tenant_more_options),
        )
    }
    DropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
        if (isArchived) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.tenant_menu_restore)) },
                onClick = {
                    isOpen = false
                    onRestoreClicked()
                },
            )
        } else {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.tenant_menu_edit_details)) },
                onClick = {
                    isOpen = false
                    onEditDetailsClicked()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.tenant_menu_archive)) },
                onClick = {
                    isOpen = false
                    onArchiveClicked()
                },
            )
        }
    }
}

@Composable
private fun TenantSections(
    tenant: TenantUiState.Content,
    onEditDetailsClicked: () -> Unit,
    onChangeRentClicked: () -> Unit,
    onRentTermClicked: (rentTermId: String) -> Unit,
    onLogPaymentClicked: () -> Unit,
    onAddChargeClicked: () -> Unit,
    onViewLedgerClicked: () -> Unit,
    onLedgerRowClicked: (key: String) -> Unit,
    onLedgerRestoreClicked: (key: String) -> Unit,
    calendarActions: CalendarActions,
    contentPadding: PaddingValues,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        listOfNotNull(tenant.syncError, tenant.statusChange.error).forEach { error ->
            Text(
                text = error.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (tenant.isArchived) ArchivedLabel()
        tenant.balance?.let { balance -> BalanceCard(balance) }
        EntryActionButtons(
            canLogPayment = tenant.canLogPayment,
            canAddCharge = tenant.canAddCharge,
            onLogPaymentClicked = onLogPaymentClicked,
            onAddChargeClicked = onAddChargeClicked,
        )
        tenant.calendar?.let { calendar ->
            CalendarCard(
                calendar = calendar,
                onPreviousYearClicked = calendarActions.onPreviousYearClicked,
                onNextYearClicked = calendarActions.onNextYearClicked,
                onMonthClicked = calendarActions.onMonthClicked,
            )
        }
        LedgerPreviewCard(
            rows = tenant.ledgerPreview,
            onRowClicked = onLedgerRowClicked,
            onRestoreClicked = onLedgerRestoreClicked,
            onViewFullLedgerClicked = onViewLedgerClicked,
        )
        RentTermsCard(
            rentTerms = tenant.rentTerms,
            isEditable = !tenant.isArchived,
            onChangeRentClicked = onChangeRentClicked,
            onRentTermClicked = onRentTermClicked,
        )
        TenantDetailsCard(
            details = tenant.details,
            isEditable = !tenant.isArchived,
            onEditClicked = onEditDetailsClicked,
        )
    }
}

@Composable
private fun ArchivedLabel() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(text = stringResource(Res.string.tenant_archived_label), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun StatusChangeDialog(
    confirmation: TenantConfirmationUiState,
    onConfirmed: () -> Unit,
    onDismissed: () -> Unit,
) {
    val (title, confirmLabel) =
        when (confirmation.action) {
            TenantStatusAction.Archive -> Res.string.tenant_archive_title to Res.string.tenant_archive_confirm
            TenantStatusAction.Restore -> Res.string.tenant_restore_title to Res.string.tenant_restore_confirm
        }
    AlertDialog(
        onDismissRequest = onDismissed,
        confirmButton = { TextButton(onClick = onConfirmed) { Text(stringResource(confirmLabel)) } },
        dismissButton = {
            TextButton(onClick = onDismissed) { Text(stringResource(Res.string.common_cancel)) }
        },
        title = { Text(stringResource(title)) },
        text = { Text(confirmation.message.asString()) },
    )
}

@Composable
private fun Loading(contentPadding: PaddingValues) {
    val description = stringResource(Res.string.tenant_loading)
    Box(modifier = Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = description })
    }
}

@Composable
private fun ErrorMessage(
    message: UiText,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = message.asString(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
    }
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
                            nextDue = UiText.Plain("Next rent due Apr 1, 2026"),
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
                            RentTermRowUiState(
                                rentTermId = "r1",
                                summary = UiText.Plain("$1,450.00 a month · due the 1st"),
                                period = UiText.Plain("Jan 2024 – Dec 2024"),
                                isCurrent = false,
                            ),
                        ),
                    details =
                        TenantDetailsUiState(
                            contactName = "Dana Whitfield",
                            phone = "(555) 014-2290",
                            email = "dana.w@example.com",
                            movedIn = "Jan 1, 2024",
                            lastDayInUnit = null,
                            securityDeposit = "$1,500.00",
                            gracePeriod = UiText.Plain("5 days (group)"),
                            lateFee = UiText.Plain("$50.00 (group)"),
                            notes = "Two roommates on the lease; Dana sends rent for both.",
                        ),
                ),
            onBackClicked = {},
            onEditDetailsClicked = {},
            onChangeRentClicked = {},
            onRentTermClicked = {},
            onLogPaymentClicked = {},
            onAddChargeClicked = {},
            onViewLedgerClicked = {},
            onLedgerRowClicked = {},
            onLedgerRestoreClicked = {},
            calendarActions = CalendarActions.None,
            onLateFeeAddClicked = {},
            onLateFeeDismissed = {},
            onArchiveClicked = {},
            onRestoreClicked = {},
            onStatusChangeConfirmed = {},
            onStatusChangeDismissed = {},
        )
    }
}
