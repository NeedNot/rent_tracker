package net.neednot.renttracker.feature.payments.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.payment_editor_conflict_named
import net.neednot.renttracker.resources.payment_editor_conflict_unnamed

internal fun conflictBanner(editedByName: String?): UiText =
    if (editedByName == null) {
        UiText.Resource(Res.string.payment_editor_conflict_unnamed)
    } else {
        UiText.Resource(Res.string.payment_editor_conflict_named, listOf(editedByName))
    }
