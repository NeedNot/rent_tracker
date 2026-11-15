package net.neednot.renttracker.feature.members.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.firstNameOf
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_conflict_named
import net.neednot.renttracker.resources.members_conflict_unnamed

internal fun membersConflictBanner(editedByName: String?): UiText =
    if (editedByName == null) {
        UiText.Resource(Res.string.members_conflict_unnamed)
    } else {
        UiText.Resource(Res.string.members_conflict_named, listOf(firstNameOf(editedByName)))
    }
