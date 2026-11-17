package net.neednot.renttracker.feature.members.presentation.viewmodel

import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.core.ui.firstNameOf
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_conflict_body

internal fun membersConflictBanner(memberName: String): UiText =
    UiText.Resource(Res.string.members_conflict_body, listOf(firstNameOf(memberName)))
