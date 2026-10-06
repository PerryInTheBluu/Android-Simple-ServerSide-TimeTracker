package com.example.util.simpletimetracker.domain.timetable.model

/**
 * A calendar subscription source (e.g. Nextcloud WebDAV ICS or remote URL).
 */
data class CalendarSubscription(
    val id: String,
    val name: String,
    val url: String,
    val color: String = "",
    val enabled: Boolean = true,
    val lastFetched: Long = 0L,
) {
    companion object {
        private const val ITEM_SEP = ";;;"
        private const val FIELD_SEP = "|||"

        fun serialize(list: List<CalendarSubscription>): String {
            return list.joinToString(ITEM_SEP) { sub ->
                listOf(
                    sub.id,
                    sub.name,
                    sub.url,
                    sub.color,
                    sub.enabled.toString(),
                    sub.lastFetched.toString(),
                ).joinToString(FIELD_SEP)
            }
        }

        fun deserialize(serialized: String): List<CalendarSubscription> {
            if (serialized.isBlank()) return emptyList()
            return serialized.split(ITEM_SEP).mapNotNull { item ->
                val parts = item.split(FIELD_SEP)
                if (parts.size >= 6) {
                    CalendarSubscription(
                        id = parts[0],
                        name = parts[1],
                        url = parts[2],
                        color = parts[3],
                        enabled = parts[4].toBooleanStrictOrNull() ?: true,
                        lastFetched = parts[5].toLongOrNull() ?: 0L,
                    )
                } else null
            }
        }
    }
}
