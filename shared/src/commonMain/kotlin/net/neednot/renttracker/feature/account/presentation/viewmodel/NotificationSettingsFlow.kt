package net.neednot.renttracker.feature.account.presentation.viewmodel

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.get
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.neednot.renttracker.feature.account.domain.AccountError
import net.neednot.renttracker.feature.account.domain.GroupNotificationSetting
import net.neednot.renttracker.feature.account.domain.NotificationSettingsRepository
import net.neednot.renttracker.feature.account.presentation.uistate.GroupNotificationsRowUiState
import net.neednot.renttracker.feature.account.presentation.uistate.NotificationsSectionUiState

internal class NotificationSettingsFlow(
    private val scope: CoroutineScope,
    private val repository: NotificationSettingsRepository,
    private val onFailure: (AccountError) -> Unit,
) {
    private val toggles = MutableStateFlow<Map<String, Toggle>>(emptyMap())
    private var latestSettings: List<GroupNotificationSetting> = emptyList()

    val section: Flow<NotificationsSectionUiState> =
        combine(repository.observeSettings().onEach(::onServerResult), toggles) { result, toggles ->
            result.fold(
                success = { settings -> settings.toSection(toggles) },
                failure = { NotificationsSectionUiState.Hidden },
            )
        }.onStart { emit(NotificationsSectionUiState.Hidden) }

    fun toggle(groupId: String) {
        val toggle = toggles.value[groupId]
        if (toggle?.status == ToggleStatus.Saving) return
        val current =
            toggle?.notificationsOn ?: latestSettings.find { it.groupId == groupId }?.notificationsOn ?: return
        val wanted = !current
        toggles.update { it + (groupId to Toggle(wanted, ToggleStatus.Saving)) }
        scope.launch {
            repository
                .setNotificationsOn(groupId, wanted)
                .onOk { toggles.update { it + (groupId to Toggle(wanted, ToggleStatus.Saved)) } }
                .onErr { error ->
                    toggles.update { it - groupId }
                    onFailure(error)
                }
        }
    }

    // A saved toggle stands in until the next server result, which already carries it.
    private fun onServerResult(result: Result<List<GroupNotificationSetting>, AccountError>) {
        latestSettings = result.get() ?: latestSettings
        toggles.update { current -> current.filterValues { it.status == ToggleStatus.Saving } }
    }

    private fun List<GroupNotificationSetting>.toSection(toggles: Map<String, Toggle>): NotificationsSectionUiState =
        NotificationsSectionUiState.Shown(
            groups =
                map { setting ->
                    GroupNotificationsRowUiState(
                        groupId = setting.groupId,
                        groupName = setting.groupName,
                        isOn = toggles[setting.groupId]?.notificationsOn ?: setting.notificationsOn,
                    )
                },
            isBlockedOnPhone = repository.areNotificationsBlockedOnPhone(),
        )

    private data class Toggle(
        val notificationsOn: Boolean,
        val status: ToggleStatus,
    )

    private enum class ToggleStatus {
        Saving,
        Saved,
    }
}
