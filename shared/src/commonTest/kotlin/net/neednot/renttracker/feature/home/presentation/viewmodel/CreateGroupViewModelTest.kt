package net.neednot.renttracker.feature.home.presentation.viewmodel

import app.cash.turbine.test
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.neednot.renttracker.core.ui.MainDispatcherTest
import net.neednot.renttracker.core.ui.UiText
import net.neednot.renttracker.feature.home.domain.FakeHomeRepository
import net.neednot.renttracker.feature.home.domain.HomeError
import net.neednot.renttracker.feature.home.presentation.uistate.CreateGroupUiState
import net.neednot.renttracker.resources.Res
import net.neednot.renttracker.resources.create_group_error_name_required
import net.neednot.renttracker.resources.home_error_offline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CreateGroupViewModelTest : MainDispatcherTest() {
    private val repository = FakeHomeRepository()
    private val viewModel by lazy { CreateGroupViewModel(repository) }

    @Test
    fun startsWithAnEmptyName() =
        runTest {
            assertEquals(CreateGroupUiState(), viewModel.uiState.value)
        }

    @Test
    fun savingCreatesTheGroupWithTheTrimmedNameAndOpensIt() =
        runTest {
            repository.createGroupResult = Ok("g7")

            viewModel.events.test {
                viewModel.onNameChanged("  1st Street ")
                viewModel.onSaveClicked()

                assertEquals(CreateGroupEvent.GroupCreated("g7"), awaitItem())
            }
            assertEquals(listOf("1st Street"), repository.createdGroupNames)
        }

    @Test
    fun aBlankNameIsRejectedBeforeSaving() =
        runTest {
            listOf("", "   ").forEach { name ->
                viewModel.onNameChanged(name)
                viewModel.onSaveClicked()
                runCurrent()

                assertEquals(
                    UiText.Resource(Res.string.create_group_error_name_required),
                    viewModel.uiState.value.error,
                )
            }
            assertTrue(repository.createdGroupNames.isEmpty())
        }

    @Test
    fun aServerRejectionShowsTheServersMessage() =
        runTest {
            repository.createGroupResult = Err(HomeError.ValidationFailed("Group name is too long."))

            viewModel.onNameChanged("1st Street")
            viewModel.onSaveClicked()
            runCurrent()

            assertEquals(
                CreateGroupUiState(name = "1st Street", error = UiText.Plain("Group name is too long.")),
                viewModel.uiState.value,
            )
        }

    @Test
    fun beingOfflineShowsTheOfflineMessageAndAllowsAnotherTry() =
        runTest {
            repository.createGroupResult = Err(HomeError.Offline)

            viewModel.onNameChanged("1st Street")
            viewModel.onSaveClicked()
            runCurrent()

            assertEquals(UiText.Resource(Res.string.home_error_offline), viewModel.uiState.value.error)
            assertEquals(false, viewModel.uiState.value.isSaving)
        }

    @Test
    fun editingTheNameClearsTheError() =
        runTest {
            viewModel.onSaveClicked()

            viewModel.onNameChanged("1")

            assertEquals(CreateGroupUiState(name = "1"), viewModel.uiState.value)
        }
}
