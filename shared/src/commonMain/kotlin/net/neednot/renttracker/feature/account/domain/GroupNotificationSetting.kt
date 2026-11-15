package net.neednot.renttracker.feature.account.domain

data class GroupNotificationSetting(
    val groupId: String,
    val groupName: String,
    val notificationsOn: Boolean,
)
