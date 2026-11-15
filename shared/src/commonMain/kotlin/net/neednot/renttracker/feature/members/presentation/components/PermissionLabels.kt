package net.neednot.renttracker.feature.members.presentation.components

import net.neednot.renttracker.core.access.MemberPermissionLabel
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.AccessLevel
import net.neednot.renttracker.feature.members.presentation.uistate.PermissionArea
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.members_area_debts
import net.neednot.renttracker.resources.members_area_members
import net.neednot.renttracker.resources.members_area_payments
import net.neednot.renttracker.resources.members_area_rent_terms
import net.neednot.renttracker.resources.members_area_settings
import net.neednot.renttracker.resources.members_area_tenants
import net.neednot.renttracker.resources.members_label_bookkeeper
import net.neednot.renttracker.resources.members_label_custom
import net.neednot.renttracker.resources.members_label_manager
import net.neednot.renttracker.resources.members_label_owner
import net.neednot.renttracker.resources.members_label_viewer
import net.neednot.renttracker.resources.members_level_add_edit
import net.neednot.renttracker.resources.members_level_full
import net.neednot.renttracker.resources.members_level_read
import org.jetbrains.compose.resources.StringResource

internal val MemberPermissionLabel.text: StringResource
    get() =
        when (this) {
            MemberPermissionLabel.Owner -> Res.string.members_label_owner
            MemberPermissionLabel.Viewer -> Res.string.members_label_viewer
            MemberPermissionLabel.Bookkeeper -> Res.string.members_label_bookkeeper
            MemberPermissionLabel.Manager -> Res.string.members_label_manager
            MemberPermissionLabel.Custom -> Res.string.members_label_custom
        }

internal val PermissionPreset.text: StringResource
    get() =
        when (this) {
            PermissionPreset.Viewer -> Res.string.members_label_viewer
            PermissionPreset.Bookkeeper -> Res.string.members_label_bookkeeper
            PermissionPreset.Manager -> Res.string.members_label_manager
        }

internal val AccessLevel.text: StringResource
    get() =
        when (this) {
            AccessLevel.Read -> Res.string.members_level_read
            AccessLevel.AddEdit -> Res.string.members_level_add_edit
            AccessLevel.Full -> Res.string.members_level_full
        }

internal val PermissionArea.text: StringResource
    get() =
        when (this) {
            PermissionArea.Tenants -> Res.string.members_area_tenants
            PermissionArea.RentTerms -> Res.string.members_area_rent_terms
            PermissionArea.Payments -> Res.string.members_area_payments
            PermissionArea.Debts -> Res.string.members_area_debts
            PermissionArea.Members -> Res.string.members_area_members
            PermissionArea.Settings -> Res.string.members_area_settings
        }
