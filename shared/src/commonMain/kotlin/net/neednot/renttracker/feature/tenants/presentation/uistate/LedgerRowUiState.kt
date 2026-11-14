package net.neednot.renttracker.feature.tenants.presentation.uistate

import net.neednot.renttracker.core.ui.UiText

data class LedgerRowUiState(
    val key: String,
    val date: String,
    val title: UiText,
    val details: List<UiText>,
    val amount: UiText,
    val balance: UiText?,
    val chips: List<EntryChip>,
    val isStruckThrough: Boolean,
    val mayNotApply: Boolean,
    val canRestore: Boolean,
    val isTappable: Boolean,
)
