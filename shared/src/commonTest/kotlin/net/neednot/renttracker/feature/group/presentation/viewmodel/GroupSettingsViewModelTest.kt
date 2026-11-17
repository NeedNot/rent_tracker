package net.neednot.renttracker.feature.group.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.access.PermissionPreset
import net.neednot.renttracker.core.data.PaymentOrder
import net.neednot.renttracker.core.data.SettingsAccessLevel
import net.neednot.renttracker.core.sync.SyncStatus
import net.neednot.renttracker.core.sync.Synced
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.group.domain.FakeGroupRepository
import net.neednot.renttracker.feature.group.domain.GroupDetails
import net.neednot.renttracker.feature.group.domain.GroupError
import net.neednot.renttracker.feature.group.domain.groupDetails
import net.neednot.renttracker.feature.group.domain.groupSettings
import net.neednot.renttracker.feature.group.domain.memberAccess
import net.neednot.renttracker.feature.group.domain.ownerAccess
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsField
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsForm
import net.neednot.renttracker.feature.group.presentation.uistate.GroupSettingsUiState
import net.neednot.renttracker.feature.group.presentation.uistate.TimeZoneOptionUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.group_settings_conflict_named
import net.neednot.renttracker.resources.group_settings_conflict_unnamed
import net.neednot.renttracker.resources.group_settings_error_grace_days
import net.neednot.renttracker.resources.group_settings_error_late_fee
import net.neednot.renttracker.resources.group_settings_error_name
import net.neednot.renttracker.resources.time_zone_pacific
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class GroupSettingsViewModelTest : MainDispatcherTest() {
    private val repository = FakeGroupRepository()
    private val viewModel by lazy { GroupSettingsViewModel("g1", repository) }

    private fun load(details: GroupDetails = groupDetails()) {
        repository.group.value = Synced(details, SyncStatus.Synced)
    }

    private fun TestScope.settle() {
        viewModel
        runCurrent()
    }

    private fun TestScope.editing(): GroupSettingsUiState.Editing {
        settle()
        return assertIs<GroupSettingsUiState.Editing>(viewModel.uiState.value)
    }

    @Test
    fun showsLoadingUntilTheGroupArrives() =
        runTest {
            assertEquals(GroupSettingsUiState.Loading, viewModel.uiState.value)
        }

    @Test
    fun anOwnerSeesTheSavedSettingsInTheForm() =
        runTest {
            load(groupDetails(settings = groupSettings(graceDays = 3, lateFeeFlatCents = 5_000)))

            val editing = editing()

            assertEquals(
                GroupSettingsForm("1st Street", "America/Los_Angeles", PaymentOrder.RentFirst, "3", "50.00"),
                editing.form,
            )
        }

    @Test
    fun aGroupWithoutASuggestedFeeShowsAnEmptyFeeField() =
        runTest {
            load(groupDetails(settings = groupSettings(lateFeeFlatCents = null)))

            assertEquals("", editing().form.lateFee)
        }

    @Test
    fun theTimeZoneListIsTheCuratedZonesPlusTheGroupsOwnWhenItIsNotInThem() =
        runTest {
            load(groupDetails(settings = groupSettings(timeZone = "Europe/London")))

            val options = editing().timeZoneOptions

            assertEquals(8, options.size)
            assertEquals(TimeZoneOptionUiState("Europe/London", UiText.Plain("Europe/London")), options.last())
            assertEquals(
                UiText.Resource(Res.string.time_zone_pacific),
                options.first { it.id == "America/Los_Angeles" }.label,
            )
        }

    @Test
    fun aMemberWithoutFullSettingsAccessSeesAReadOnlyView() =
        runTest {
            load(groupDetails(access = memberAccess(PermissionPreset.Manager.permissions)))
            settle()

            val readOnly = assertIs<GroupSettingsUiState.ReadOnly>(viewModel.uiState.value)

            assertEquals(
                GroupSettingsForm("1st Street", "America/Los_Angeles", PaymentOrder.RentFirst, "5", "50.00"),
                readOnly.form,
            )
            assertEquals(
                UiText.Resource(Res.string.time_zone_pacific),
                readOnly.timeZoneOptions.first { it.id == readOnly.form.timeZone }.label,
            )
        }

    @Test
    fun aReadOnlyViewWithoutASuggestedFeeLeavesTheFeeEmpty() =
        runTest {
            load(groupDetails(access = memberAccess(), settings = groupSettings(lateFeeFlatCents = null)))
            settle()

            assertEquals("", assertIs<GroupSettingsUiState.ReadOnly>(viewModel.uiState.value).form.lateFee)
        }

    @Test
    fun aMemberWithFullSettingsAccessCanEdit() =
        runTest {
            val permissions = PermissionPreset.Manager.permissions.copy(settings = SettingsAccessLevel.Full)
            load(groupDetails(access = memberAccess(permissions)))

            editing()
        }

    @Test
    fun anArchivedGroupIsReadOnlyEvenForTheOwner() =
        runTest {
            load(groupDetails(access = ownerAccess(groupArchived = true)))
            settle()

            assertIs<GroupSettingsUiState.ReadOnly>(viewModel.uiState.value)
        }

    @Test
    fun aBlankNameIsFlaggedWhenSaving() =
        runTest {
            load()
            editing()

            viewModel.onFieldChanged(GroupSettingsField.Name, "  ")
            viewModel.onSaveClicked()

            assertEquals(
                mapOf(GroupSettingsField.Name to UiText.Resource(Res.string.group_settings_error_name)),
                (viewModel.uiState.value as GroupSettingsUiState.Editing).fieldErrors,
            )
            assertEquals(emptyList(), repository.updatedSettings)
        }

    @Test
    fun graceDaysAboveFifteenAreFlaggedAsTyped() =
        runTest {
            load()
            editing()

            viewModel.onFieldChanged(GroupSettingsField.GraceDays, "20")

            assertEquals(
                mapOf(GroupSettingsField.GraceDays to UiText.Resource(Res.string.group_settings_error_grace_days)),
                (viewModel.uiState.value as GroupSettingsUiState.Editing).fieldErrors,
            )
        }

    @Test
    fun aLateFeeOfZeroIsFlaggedAsTyped() =
        runTest {
            load()
            editing()

            viewModel.onFieldChanged(GroupSettingsField.LateFee, "0.00")

            assertEquals(
                mapOf(GroupSettingsField.LateFee to UiText.Resource(Res.string.group_settings_error_late_fee)),
                (viewModel.uiState.value as GroupSettingsUiState.Editing).fieldErrors,
            )
        }

    @Test
    fun anEmptyLateFeeIsAccepted() =
        runTest {
            load()
            editing()

            viewModel.onFieldChanged(GroupSettingsField.LateFee, "")

            assertEquals(emptyMap(), (viewModel.uiState.value as GroupSettingsUiState.Editing).fieldErrors)
        }

    @Test
    fun aMissingNameIsNotFlaggedUntilTheFirstSave() =
        runTest {
            load()
            editing()

            viewModel.onFieldChanged(GroupSettingsField.Name, "")

            assertEquals(emptyMap(), (viewModel.uiState.value as GroupSettingsUiState.Editing).fieldErrors)
        }

    @Test
    fun savingSendsTheEditedSettingsWithTheVersionThatWasLoaded() =
        runTest {
            load(groupDetails(version = 7))
            editing()

            viewModel.onFieldChanged(GroupSettingsField.Name, " 2nd Street ")
            viewModel.onTimeZoneSelected("America/Chicago")
            viewModel.onPaymentOrderSelected(PaymentOrder.ByDate)
            viewModel.onFieldChanged(GroupSettingsField.GraceDays, "10")
            viewModel.onFieldChanged(GroupSettingsField.LateFee, "")
            viewModel.events.test {
                viewModel.onSaveClicked()

                assertEquals(GroupSettingsEvent.Saved, awaitItem())
            }

            val expected = groupSettings("2nd Street", "America/Chicago", PaymentOrder.ByDate, 10, null)
            assertEquals(listOf(Triple("g1", 7L, expected)), repository.updatedSettings)
        }

    @Test
    fun aServerValidationErrorIsShownUnderItsField() =
        runTest {
            load()
            editing()
            repository.updateSettingsResult = Err(GroupError.ValidationFailed("Group name is required.", "Group name"))

            viewModel.onSaveClicked()
            settle()

            assertEquals(
                mapOf(GroupSettingsField.Name to UiText.Plain("Group name is required.")),
                (viewModel.uiState.value as GroupSettingsUiState.Editing).fieldErrors,
            )
        }

    @Test
    fun anOfflineSaveShowsTheErrorAndKeepsTheForm() =
        runTest {
            load()
            editing()
            repository.updateSettingsResult = Err(GroupError.Offline)

            viewModel.onSaveClicked()
            settle()

            val editing = viewModel.uiState.value as GroupSettingsUiState.Editing
            assertEquals(false, editing.isSaving)
            assertEquals(groupSettings().name, editing.form.name)
            assertIs<UiText.Resource>(editing.error)
        }

    @Test
    fun aConflictReloadsTheServersValuesAndNamesWhoChangedThem() =
        runTest {
            load(groupDetails(version = 3, settings = groupSettings(graceDays = 5)))
            editing()
            repository.updateSettingsResult = Err(GroupError.EditConflict("changed", "Sam Rivera"))
            repository.group.value =
                Synced(groupDetails(version = 4, settings = groupSettings(graceDays = 3)), SyncStatus.Synced)

            viewModel.onFieldChanged(GroupSettingsField.GraceDays, "9")
            viewModel.onSaveClicked()
            settle()

            val editing = viewModel.uiState.value as GroupSettingsUiState.Editing
            assertEquals("3", editing.form.graceDays)
            assertEquals(
                UiText.Resource(Res.string.group_settings_conflict_named, listOf("Sam Rivera")),
                editing.conflictTitle,
            )
            repository.updateSettingsResult =
                com.github.michaelbull.result
                    .Ok(Unit)
            viewModel.onSaveClicked()
            settle()
            assertEquals(4L, repository.updatedSettings.last().second)
        }

    @Test
    fun aConflictWithoutAKnownEditorUsesTheUnnamedBanner() =
        runTest {
            load()
            editing()
            repository.updateSettingsResult = Err(GroupError.EditConflict("changed", null))

            viewModel.onSaveClicked()
            settle()

            assertEquals(
                UiText.Resource(Res.string.group_settings_conflict_unnamed),
                (viewModel.uiState.value as GroupSettingsUiState.Editing).conflictTitle,
            )
        }

    @Test
    fun aFailureWithNothingCachedShowsTheError() =
        runTest {
            repository.group.value = Synced(null, SyncStatus.Failed(GroupError.Offline))
            settle()

            assertIs<GroupSettingsUiState.Error>(viewModel.uiState.value)
        }
}
