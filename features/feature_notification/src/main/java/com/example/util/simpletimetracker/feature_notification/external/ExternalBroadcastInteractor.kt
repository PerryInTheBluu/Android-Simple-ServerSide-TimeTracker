package com.example.util.simpletimetracker.feature_notification.external

import com.example.util.simpletimetracker.domain.color.model.AppColor
import com.example.util.simpletimetracker.domain.extension.orEmpty
import com.example.util.simpletimetracker.domain.record.interactor.AddRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.domain.recordTag.interactor.GetSelectableTagsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordsUpdateInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RemoveRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.domain.notifications.model.ExternalActionCommentMode
import com.example.util.simpletimetracker.domain.notifications.model.ExternalActionFindRecordMode
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTagValueType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

class ExternalBroadcastInteractor @Inject constructor(
    private val recordTypeInteractor: RecordTypeInteractor,
    private val addRunningRecordMediator: AddRunningRecordMediator,
    private val addRecordMediator: AddRecordMediator,
    private val removeRunningRecordMediator: RemoveRunningRecordMediator,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val recordInteractor: RecordInteractor,
    private val getSelectableTagsInteractor: GetSelectableTagsInteractor,
    private val recordsUpdateInteractor: RecordsUpdateInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val timetableRepo: com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo,
) {

    suspend fun onActionActivityStart(
        name: String,
        comment: String?,
        tagNames: List<String>,
        timeStarted: String?,
        offsetMinutes: Long? = null,
    ) {
        val typeId = getTypeIdByName(name) ?: return
        val runningRecord = runningRecordInteractor.get(typeId)
        if (runningRecord != null) return // Already running.
        val tagIds = findTagIdByName(tagNames, typeId)
        val now = System.currentTimeMillis()
        val newTimeStarted = if (offsetMinutes != null) {
            now - abs(offsetMinutes) * 60_000L
        } else {
            timeStarted?.let { parseTimestamp(it, now) }
        }

        addRunningRecordMediator.startTimer(
            typeId = typeId,
            comment = comment.orEmpty(),
            tags = mapTagIdsToTags(tagIds),
            timeStarted = if (newTimeStarted != null) {
                AddRunningRecordMediator.StartTime.Timestamp(newTimeStarted)
            } else {
                AddRunningRecordMediator.StartTime.TakeCurrent
            },
        )
    }

    suspend fun onActionActivityStopByName(
        name: String,
        timeEnded: String?,
        offsetMinutes: Long? = null,
        durationMinutes: Long? = null,
    ) {
        val typeId = getTypeIdByName(name) ?: return
        val runningRecord = runningRecordInteractor.get(typeId)
            ?: return // Not running.
        val now = System.currentTimeMillis()
        val newTimeEnded = when {
            offsetMinutes != null -> now - abs(offsetMinutes) * 60_000L
            durationMinutes != null -> runningRecord.timeStarted + abs(durationMinutes) * 60_000L
            timeEnded != null -> parseTimestamp(timeEnded, now)
            else -> null
        }

        removeRunningRecordMediator.removeWithRecordAdd(
            runningRecord = runningRecord,
            timeEnded = newTimeEnded,
        )
    }

    suspend fun onActionActivityStopAll() {
        runningRecordInteractor.getAll()
            .forEach { removeRunningRecordMediator.removeWithRecordAdd(it) }
    }

    suspend fun onActionActivityStopShortest() {
        runningRecordInteractor.getAll()
            .maxByOrNull { it.timeStarted }
            ?.let { removeRunningRecordMediator.removeWithRecordAdd(it) }
    }

    suspend fun onActionActivityStopLongest() {
        runningRecordInteractor.getAll()
            .minByOrNull { it.timeStarted }
            ?.let { removeRunningRecordMediator.removeWithRecordAdd(it) }
    }

    suspend fun onActionActivityRestart(
        comment: String?,
        tagNames: List<String>,
    ) {
        val previousRecord = recordInteractor.getPrev(
            timeStarted = System.currentTimeMillis(),
        ) ?: return
        val typeId = previousRecord.typeId
        val tagIds = findTagIdByName(tagNames, typeId)

        addRunningRecordMediator.startTimer(
            typeId = typeId,
            comment = comment
                ?: previousRecord.comment,
            tags = tagIds
                .takeUnless { tagNames.isEmpty() }
                ?.let(::mapTagIdsToTags)
                ?: previousRecord.tags,
        )
    }

    suspend fun onRecordAdd(
        name: String,
        timeStarted: String?,
        timeEnded: String?,
        durationMinutes: Long? = null,
        offsetMinutes: Long? = null,
        comment: String?,
        tagNames: List<String>,
    ) {
        val typeId = getTypeIdByName(name) ?: return
        val now = System.currentTimeMillis()

        val (start, end) = when {
            durationMinutes != null -> {
                val durMs = abs(durationMinutes) * 60_000L
                if (timeStarted != null) {
                    val s = parseTimestamp(timeStarted, now) ?: (now - durMs)
                    s to (s + durMs)
                } else if (timeEnded != null) {
                    val e = parseTimestamp(timeEnded, now) ?: now
                    (e - durMs) to e
                } else {
                    val offMs = offsetMinutes?.let { abs(it) * 60_000L } ?: 0L
                    val e = now - offMs
                    (e - durMs) to e
                }
            }
            timeStarted != null && timeEnded != null -> {
                val s = parseTimestamp(timeStarted, now) ?: return
                val e = parseTimestamp(timeEnded, now) ?: return
                s to e
            }
            offsetMinutes != null -> {
                // If only offset is given without duration, treat offset as elapsed duration
                val durMs = abs(offsetMinutes) * 60_000L
                (now - durMs) to now
            }
            else -> return
        }

        if (end <= start) return

        val tagIds = findTagIdByName(tagNames, typeId)

        Record(
            id = 0, // Zero creates new record.
            typeId = typeId,
            timeStarted = start,
            timeEnded = end,
            comment = comment.orEmpty(),
            tags = mapTagIdsToTags(tagIds),
        ).let {
            addRecordMediator.add(it)
            recordsUpdateInteractor.send()
        }
    }

    suspend fun onRecordRescue(
        name: String?,
        minutes: Long?,
        comment: String?,
        tagNames: List<String>,
    ) {
        val now = System.currentTimeMillis()

        // 1. Resolve activity type: specified -> active/recent timetable slot -> last tracked
        val typeId = if (!name.isNullOrBlank()) {
            getTypeIdByName(name)
                ?: findActiveOrRecentTimetableActivity()
                ?: recordInteractor.getPrev(now)?.typeId
                ?: recordTypeInteractor.getAll().firstOrNull()?.id
        } else {
            findActiveOrRecentTimetableActivity()
                ?: recordInteractor.getPrev(now)?.typeId
                ?: recordTypeInteractor.getAll().firstOrNull()?.id
        } ?: return

        // 2. Resolve duration in minutes: specified -> gap since last record -> default 30 min
        val durationMin = if (minutes != null && minutes > 0L) {
            abs(minutes)
        } else {
            val prev = recordInteractor.getPrev(now)
            if (prev != null && prev.timeEnded < now) {
                ((now - prev.timeEnded) / 60_000L).coerceIn(5L, 180L)
            } else {
                30L
            }
        }

        val end = now
        val start = end - durationMin * 60_000L
        if (end <= start) return

        val tagIds = findTagIdByName(tagNames, typeId)
        val defaultComment = comment ?: "Rettung"

        Record(
            id = 0,
            typeId = typeId,
            timeStarted = start,
            timeEnded = end,
            comment = defaultComment,
            tags = mapTagIdsToTags(tagIds),
        ).let {
            addRecordMediator.add(it)
            recordsUpdateInteractor.send()
        }
    }

    private suspend fun findActiveOrRecentTimetableActivity(): Long? {
        return runCatching {
            val cal = Calendar.getInstance()
            val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
            val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val events = timetableRepo.getEvents(dayOfWeek)
            // Look for a lecture or exercise ongoing now, or ended within the last 60 minutes
            val matching = events.firstOrNull { ev ->
                ev.activityTypeId != null && minuteOfDay in (ev.startTime - 15)..(ev.endTime + 60)
            }
            matching?.activityTypeId
        }.getOrNull()
    }

    suspend fun onRecordChange(
        findModeData: String?,
        name: String?,
        comment: String?,
        commentModeData: String?,
    ) {
        val typeId = name?.let { getTypeIdByName(it) }
        val findMode = ExternalActionFindRecordMode.entries.firstOrNull {
            it.dataValue == findModeData
        } ?: ExternalActionFindRecordMode.CURRENT_OR_LAST
        val commentMode = ExternalActionCommentMode.entries.firstOrNull {
            it.dataValue == commentModeData
        } ?: ExternalActionCommentMode.SET

        fun processComment(
            oldComment: String,
            newComment: String,
        ): String {
            return when (commentMode) {
                ExternalActionCommentMode.SET -> newComment
                ExternalActionCommentMode.APPEND -> oldComment + newComment
                ExternalActionCommentMode.PREFIX -> newComment + oldComment
            }
        }

        suspend fun changeCurrent(): Boolean {
            var wasChanged = false
            runningRecordInteractor.getAll().let { allRecords ->
                if (typeId != null) allRecords.filter { it.id == typeId } else allRecords
            }.forEach { record ->
                record.copy(
                    comment = processComment(
                        oldComment = record.comment,
                        newComment = comment.orEmpty(),
                    ),
                ).let { runningRecordInteractor.add(it) }
                wasChanged = true
            }
            return wasChanged
        }

        suspend fun changeLast() {
            recordInteractor.getAllPrev(System.currentTimeMillis()).let { allRecords ->
                if (typeId != null) allRecords.filter { it.typeId == typeId } else allRecords
            }.forEach { record ->
                record.copy(
                    comment = processComment(
                        oldComment = record.comment,
                        newComment = comment.orEmpty(),
                    ),
                ).let { recordInteractor.add(it) }
            }
            recordsUpdateInteractor.send()
        }

        when (findMode) {
            ExternalActionFindRecordMode.CURRENT_OR_LAST -> {
                val currentWasChanged = changeCurrent()
                if (!currentWasChanged) changeLast()
            }
            ExternalActionFindRecordMode.CURRENT -> changeCurrent()
            ExternalActionFindRecordMode.LAST -> changeLast()
        }
    }

    suspend fun onRecordTagAdd(
        name: String?,
        icon: String?,
    ) {
        if (name == null) return

        val tagExists = recordTagInteractor.get(name).isNotEmpty()

        if (tagExists) return

        RecordTag(
            name = name,
            icon = icon.orEmpty(),
            color = AppColor(0, ""),
            iconColorSource = 0,
            note = "",
            valueType = RecordTagValueType.NONE,
            valueSuffix = "",
        ).let {
            recordTagInteractor.add(it)
        }
    }

    private suspend fun getTypeIdByName(name: String): Long? {
        return recordTypeInteractor.getAll().firstOrNull { it.name == name }?.id
    }

    private suspend fun findTagIdByName(
        names: List<String>,
        typeId: Long,
    ): List<Long> {
        if (names.isEmpty()) return emptyList()
        return getSelectableTagsInteractor.execute(typeId)
            .filter { it.name in names && !it.archived }
            .map { it.id }
            .orEmpty()
    }

    /**
     * Supported formats:
     * - Relative offsets: "-15", "-15m", "+10", "15m", "1h", "2.5h"
     * - Absolute time of day: "HH:mm", "HH:mm:ss" (interpreted as today)
     * - Standard datetime: [dateTimeFormat] ("yyyy-MM-dd HH:mm:ss")
     * - UTC epoch in milliseconds or seconds.
     * - Small pure integer (< 100_000): treated as minutes ago.
     */
    private fun parseTimestamp(timeString: String, now: Long = System.currentTimeMillis()): Long? {
        val trimmed = timeString.trim()
        if (trimmed.isEmpty()) return null

        // 1. Check relative minutes / hours e.g. "-15", "-15m", "+10", "15m", "2h", "-1h"
        val relativePattern = Regex("^([+-]?\\d+(?:\\.\\d+)?)\\s*(m|min|minutes?|h|hours?)?$", RegexOption.IGNORE_CASE)
        val match = relativePattern.matchEntire(trimmed)
        if (match != null) {
            val num = match.groupValues[1].toDoubleOrNull()
            val unit = match.groupValues[2].lowercase()
            if (num != null) {
                val isHour = unit.startsWith("h")
                val minutes = if (isHour) num * 60.0 else num
                val deltaMs = (minutes * 60_000.0).toLong()

                // If explicitly negative (e.g. -15), subtract from now
                if (num < 0) {
                    return now + deltaMs // deltaMs is already negative
                }
                // If has unit (e.g. "15m", "2h") or explicit "+"
                if (unit.isNotEmpty() || trimmed.startsWith("+")) {
                    return if (trimmed.startsWith("+")) now + abs(deltaMs) else now - abs(deltaMs)
                }
                // Small pure positive number (< 100_000): treat as minutes ago
                if (num < 100_000.0) {
                    return now - abs(deltaMs)
                }
            }
        }

        // 2. Full date format "yyyy-MM-dd HH:mm:ss"
        val parsedFull = parseDateTime(trimmed)
        if (parsedFull != null) return parsedFull

        // 3. Time of day format "HH:mm:ss" or "HH:mm" (interpreted as today)
        val timeOfDay = parseTimeOfDay(trimmed, now)
        if (timeOfDay != null) return timeOfDay

        // 4. Epoch timestamps
        val rawNum = trimmed.toLongOrNull()
        if (rawNum != null) {
            return when {
                rawNum >= 10_000_000_000L -> rawNum // Milliseconds
                rawNum >= 1_000_000_000L -> rawNum * 1000L // Seconds
                else -> now - rawNum * 60_000L // Small number -> minutes ago
            }
        }

        return null
    }

    private fun parseDateTime(timeString: String): Long? {
        return synchronized(dateTimeFormat) {
            runCatching {
                dateTimeFormat.parse(timeString)
            }.getOrNull()?.time
        }
    }

    private fun parseTimeOfDay(timeString: String, now: Long): Long? {
        val parts = timeString.split(':')
        if (parts.size in 2..3) {
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            val s = parts.getOrNull(2)?.toIntOrNull() ?: 0
            if (h in 0..23 && m in 0..59 && s in 0..59) {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, s)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }
        }
        return null
    }

    private fun mapTagIdsToTags(ids: List<Long>): List<RecordBase.Tag> {
        return ids.map {
            RecordBase.Tag(
                tagId = it,
                numericValue = null, // TODO value selection?
            )
        }
    }

    companion object {
        private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }
}