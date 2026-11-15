package net.neednot.renttracker.push

data class LateRentPush(
    val groupId: String,
    val title: String,
    val body: String,
) {
    companion object {
        private const val TYPE = "lateRent"

        fun fromData(data: Map<String, String>): LateRentPush? {
            val groupId = data["groupId"]
            val title = data["title"]
            val body = data["body"]
            val isLateRent = data["type"] == TYPE && groupId != null && title != null && body != null
            return if (isLateRent) LateRentPush(groupId, title, body) else null
        }
    }
}
