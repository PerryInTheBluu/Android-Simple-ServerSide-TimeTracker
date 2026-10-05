package com.example.util.simpletimetracker.feature_records.interactor

import com.example.util.simpletimetracker.core.interactor.GetRunningRecordViewDataMediator
import com.example.util.simpletimetracker.core.interactor.DailyRecordFilterInteractor
import com.example.util.simpletimetracker.core.interactor.DailyRecordFilterInteractor.RecordHolder
import com.example.util.simpletimetracker.core.mapper.CalendarToListShiftMapper
import com.example.util.simpletimetracker.core.mapper.ColorMapper
import com.example.util.simpletimetracker.core.mapper.RecordViewDataMapper
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.domain.base.DurationFormat
import com.example.util.simpletimetracker.domain.base.UNTRACKED_ITEM_ID
import com.example.util.simpletimetracker.domain.category.interactor.RecordTypeCategoryInteractor
import com.example.util.simpletimetracker.domain.category.model.RecordTypeCategory
import com.example.util.simpletimetracker.domain.daysOfWeek.mapper.DaysInCalendarMapper
import com.example.util.simpletimetracker.domain.extension.dropSeconds
import com.example.util.simpletimetracker.domain.extension.toRange
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.recordTag.interactor.RecordTagInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeGoalInteractor
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.domain.record.mapper.RangeMapper
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DayOfWeek
import com.example.util.simpletimetracker.domain.daysOfWeek.model.DaysInCalendar
import com.example.util.simpletimetracker.domain.record.model.Range
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.recordTag.model.RecordTag
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.domain.recordType.model.RecordTypeGoal
import com.example.util.simpletimetracker.domain.record.model.RunningRecord
import com.example.util.simpletimetracker.domain.record.interactor.GetUntrackedRecordsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor.GetParam
import com.example.util.simpletimetracker.domain.record.interactor.RecordsContainerMultiselectInteractor
import com.example.util.simpletimetracker.domain.record.model.MultiSelectedRecordId
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEvent
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.record.RecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.recordSelected.RecordSelectedViewData
import com.example.util.simpletimetracker.feature_base_adapter.runningRecord.RunningRecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.runningRecordSelected.RunningRecordSelectedViewData
import com.example.util.simpletimetracker.feature_base_adapter.timetableEvent.TimetableViewData
import com.example.util.simpletimetracker.feature_records.customView.RecordsCalendarViewData
import com.example.util.simpletimetracker.feature_records.mapper.RecordsViewDataMapper
import com.example.util.simpletimetracker.feature_records.mapper.TimetableViewDataMapper
import com.example.util.simpletimetracker.feature_records.model.RecordsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Long.min
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max

class RecordsViewDataInteractor @Inject constructor(
    private val recordInteractor: RecordInteractor,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val recordTagInteractor: RecordTagInteractor,
    private val recordTypeGoalInteractor: RecordTypeGoalInteractor,
    private val prefsInteractor: PrefsInteractor,
    private val getUntrackedRecordsInteractor: GetUntrackedRecordsInteractor,
    private val recordsViewDataMapper: RecordsViewDataMapper,
    private val recordViewDataMapper: RecordViewDataMapper,
    private val timeMapper: TimeMapper,
    private val rangeMapper: RangeMapper,
    private val getRunningRecordViewDataMediator: GetRunningRecordViewDataMediator,
    private val calendarToListShiftMapper: CalendarToListShiftMapper,
    private val recordTypeCategoryInteractor: RecordTypeCategoryInteractor,
    private val daysInCalendarMapper: DaysInCalendarMapper,
    private val recordsContainerMultiselectInteractor: RecordsContainerMultiselectInteractor,
    private val dailyRecordFilterInteractor: DailyRecordFilterInteractor,
    private val timetableRepo: TimetableRepo,
    private val timetableViewDataMapper: TimetableViewDataMapper,
    private val colorMapper: ColorMapper,
) {

    suspend fun getViewData(
        shift: Int,
        forSharing: Boolean,
    ): RecordsState = withContext(Dispatchers.Default) {
        val calendar = Calendar.getInstance()
        val isDarkTheme = prefsInteractor.getDarkMode()
        val useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat()
        val durationFormat = prefsInteractor.getDurationFormat()
        val showSeconds = prefsInteractor.getShowSeconds()
        val isMilitary = prefsInteractor.getUseMilitaryTimeFormat()
        val startOfDayShift = prefsInteractor.getStartOfDayShift()
        val endOfDayShift = prefsInteractor.getEndOfDayShift()
        val firstDayOfWeek = prefsInteractor.getFirstDayOfWeek()
        val showUntrackedInRecords = prefsInteractor.getShowUntrackedInRecords()
        val reverseOrder = prefsInteractor.getReverseOrderInCalendar()
        val recordTypes = recordTypeInteractor.getAll().associateBy(RecordType::id)
        val filterType = prefsInteractor.getListFilterType()
        val recordTags = recordTagInteractor.getAll()
        val goals = recordTypeGoalInteractor.getAllTypeGoals().groupBy { it.idData.value }
        val runningRecords = runningRecordInteractor.getAll()
        val recordTypeCategories = suspend { recordTypeCategoryInteractor.getAll() }
        val filteredIds = prefsInteractor.getListFilteredIds(filterType)
        val isCalendarView = prefsInteractor.getShowRecordsCalendar()
        val daysInCalendar = if (isCalendarView) {
            prefsInteractor.getDaysInCalendar()
        } else {
            DaysInCalendar.ONE
        }
        val daysCountInShift = daysInCalendarMapper.mapDaysCount(daysInCalendar)
        val multiSelectedIds = recordsContainerMultiselectInteractor.selectedRecordIds

        return@withContext (daysCountInShift - 1 downTo 0).map { dayInShift ->
            val actualShift = calendarToListShiftMapper.mapCalendarToListShift(
                calendarShift = shift,
                daysInCalendar = daysInCalendar,
                startOfDayShift = startOfDayShift,
                firstDayOfWeek = firstDayOfWeek,
            ).end - dayInShift

            val range = timeMapper.getRangeStartAndEnd(
                rangeLength = RangeLength.Day,
                shift = actualShift,
                firstDayOfWeek = DayOfWeek.MONDAY, // Doesn't matter for days.
                startOfDayShift = startOfDayShift,
            )
            val records = recordInteractor.getWithParams(GetParam.FromRange(range))

            val data = getRecordsViewData(
                includeTimetable = !isCalendarView && !forSharing,
                records = records,
                runningRecords = runningRecords,
                filterType = filterType,
                filteredIds = filteredIds,
                recordTypes = recordTypes,
                recordTags = recordTags,
                goals = goals,
                recordTypeCategories = recordTypeCategories,
                range = range,
                isDarkTheme = isDarkTheme,
                useMilitaryTime = useMilitaryTime,
                durationFormat = durationFormat,
                showUntrackedInRecords = showUntrackedInRecords,
                showSeconds = showSeconds,
            )

            val slots = getTimetableSlots(
                range = range,
                records = records,
                runningRecords = runningRecords,
                recordTypes = recordTypes,
                isDarkTheme = isDarkTheme,
            )

            ViewDataIntermediate(
                rangeStart = range.timeStarted,
                rangeEnd = range.timeEnded,
                isToday = actualShift == 0,
                records = data,
                slots = slots,
            )
        }.let { data ->
            if (isCalendarView) {
                mapCalendarData(
                    data = data,
                    calendar = calendar,
                    startOfDayShift = startOfDayShift,
                    endOfDayShift = endOfDayShift,
                    shift = shift,
                    reverseOrder = reverseOrder,
                    showSeconds = showSeconds,
                    isMilitary = isMilitary,
                    multiSelectedIds = multiSelectedIds,
                )
            } else {
                mapRecordsData(
                    data = data,
                    shift = shift,
                    forSharing = forSharing,
                    multiSelectedIds = multiSelectedIds,
                )
            }
        }
    }

    private fun mapCalendarData(
        data: List<ViewDataIntermediate>,
        calendar: Calendar,
        startOfDayShift: Long,
        endOfDayShift: Long = 0L,
        shift: Int,
        reverseOrder: Boolean,
        showSeconds: Boolean,
        isMilitary: Boolean,
        multiSelectedIds: List<MultiSelectedRecordId>,
    ): RecordsState.CalendarData.Data {
        val currentTime = if (shift == 0) {
            timeMapper.mapFromStartOfDay(
                timeStamp = System.currentTimeMillis(),
                calendar = calendar,
            ) - startOfDayShift
        } else {
            null
        }
        val shouldMapLegends = data.size > 1

        return data
            .map { column ->
                val legend = if (shouldMapLegends) {
                    timeMapper.getDayOfWeek(
                        timestamp = column.rangeStart,
                        calendar = calendar,
                        startOfDayShift = startOfDayShift,
                    ).let(timeMapper::toShortDayOfWeekName)
                } else {
                    ""
                }

                val points = column.records.map { record ->
                    mapToCalendarPoint(
                        holder = record,
                        calendar = calendar,
                        startOfDayShift = startOfDayShift,
                        rangeStart = column.rangeStart,
                        rangeEnd = column.rangeEnd,
                        showSeconds = showSeconds,
                        multiSelectedIds = multiSelectedIds,
                    )
                }

                RecordsCalendarViewData.Points(
                    legend = legend,
                    highlighted = column.isToday,
                    data = points,
                    slots = column.slots,
                )
            }
            .let { list ->
                RecordsCalendarViewData(
                    currentTime = currentTime,
                    startOfDayShift = startOfDayShift,
                    endOfDayShift = endOfDayShift,
                    points = list,
                    reverseOrder = reverseOrder,
                    shouldDrawTopLegends = shouldMapLegends,
                    isMilitary = isMilitary,
                )
            }
            .let(RecordsState.CalendarData::Data)
    }

    private suspend fun mapRecordsData(
        data: List<ViewDataIntermediate>,
        shift: Int,
        forSharing: Boolean,
        multiSelectedIds: List<MultiSelectedRecordId>,
    ): RecordsState.RecordsData {
        val records = data.firstOrNull()?.records.orEmpty()

        val showFirstEnterHint = when {
            // Show hint only on current date.
            shift != 0 -> false
            // Check all records only if there is no records for this day.
            records.isNotEmpty() -> false
            // Try to find if any record exists.
            else -> recordInteractor.isEmpty() && runningRecordInteractor.isEmpty()
        }

        val hint = if (!forSharing) {
            recordsViewDataMapper.mapToHint()
        } else {
            null
        }

        val items = when {
            showFirstEnterHint -> listOf(recordViewDataMapper.mapToNoRecords())
            records.isEmpty() -> listOf(recordViewDataMapper.mapToEmpty())
            else -> {
                dailyRecordFilterInteractor.sort(records)
                    .map { remapForMultiselect(it, multiSelectedIds) } +
                    listOfNotNull(hint)
            }
        }

        return RecordsState.RecordsData(items)
    }

    private fun remapForMultiselect(
        holder: RecordHolder<Data>,
        multiSelectedIds: List<MultiSelectedRecordId>,
    ): ViewHolderType {
        // If disabled - return right away.
        if (multiSelectedIds.isEmpty()) {
            return holder.data.value
        }
        val multiSelectedId = mapMultiSelectedId(holder)
        return when (val data = holder.data) {
            is Data.RecordData -> {
                val value = data.value
                if (multiSelectedId in multiSelectedIds) {
                    RecordSelectedViewData(value)
                } else {
                    value
                }
            }
            is Data.RunningRecordData -> {
                val value = data.value
                if (multiSelectedId in multiSelectedIds) {
                    RunningRecordSelectedViewData(value)
                } else {
                    value
                }
            }
            is Data.TimetableEventData -> data.value
        }
    }

    private suspend fun getRecordsViewData(
        includeTimetable: Boolean,
        records: List<Record>,
        runningRecords: List<RunningRecord>,
        filterType: ChartFilterType,
        filteredIds: List<Long>,
        recordTypes: Map<Long, RecordType>,
        recordTags: List<RecordTag>,
        goals: Map<Long, List<RecordTypeGoal>>,
        recordTypeCategories: suspend () -> List<RecordTypeCategory>,
        range: Range,
        isDarkTheme: Boolean,
        useMilitaryTime: Boolean,
        durationFormat: DurationFormat,
        showUntrackedInRecords: Boolean,
        showSeconds: Boolean,
    ): List<RecordHolder<Data>> {
        val trackedRecordsData = records
            .map { record ->
                recordsViewDataMapper.map(
                    record = record,
                    recordType = recordTypes[record.typeId],
                    recordTags = recordTags,
                    range = range,
                    isDarkTheme = isDarkTheme,
                    useMilitaryTime = useMilitaryTime,
                    durationFormat = durationFormat,
                    showSeconds = showSeconds,
                ).let {
                    RecordHolder<Data>(
                        timeStartedTimestamp = it.timeStartedTimestamp,
                        typeId = record.typeId,
                        tagIds = record.tags.map(RecordBase.Tag::tagId),
                        data = Data.RecordData(it),
                    )
                }
            }

        val runningRecordsData = runningRecords
            .let {
                rangeMapper.getRunningRecordsFromRange(it, range)
            }
            .mapNotNull { runningRecord ->
                getRunningRecordViewDataMediator.execute(
                    type = recordTypes[runningRecord.id] ?: return@mapNotNull null,
                    tags = recordTags,
                    goals = goals[runningRecord.id].orEmpty(),
                    record = runningRecord,
                    nowIconVisible = true,
                    goalsVisible = false,
                    totalDurationVisible = false,
                    isDarkTheme = isDarkTheme,
                    useMilitaryTime = useMilitaryTime,
                    durationFormat = durationFormat,
                    showSeconds = showSeconds,
                ).let {
                    RecordHolder<Data>(
                        timeStartedTimestamp = it.timeStartedTimestamp,
                        typeId = runningRecord.id,
                        tagIds = runningRecord.tags.map(RecordBase.Tag::tagId),
                        data = Data.RunningRecordData(it),
                    )
                }
            }

        val untrackedRecordsData = if (
            showUntrackedInRecords &&
            UNTRACKED_ITEM_ID !in filteredIds
        ) {
            val recordRanges = records.map(Record::toRange)
            val runningRecordRanges = runningRecords.map(RunningRecord::toRange)
            getUntrackedRecordsInteractor.get(
                range = range,
                records = recordRanges + runningRecordRanges,
            ).map { untrackedRecord ->
                recordsViewDataMapper.mapToUntracked(
                    record = untrackedRecord,
                    range = range,
                    isDarkTheme = isDarkTheme,
                    useMilitaryTime = useMilitaryTime,
                    durationFormat = durationFormat,
                    showSeconds = showSeconds,
                ).let {
                    RecordHolder<Data>(
                        timeStartedTimestamp = it.timeStartedTimestamp,
                        typeId = UNTRACKED_ITEM_ID,
                        tagIds = emptyList(),
                        data = Data.RecordData(it),
                    )
                }
            }
        } else {
            emptyList()
        }

        val holders = dailyRecordFilterInteractor.filter(
            runningRecordsData = runningRecordsData,
            trackedRecordsData = trackedRecordsData,
            untrackedRecordsData = untrackedRecordsData,
            chartFilterType = filterType,
            filteredIds = filteredIds,
            lazyRecordTypeCategories = recordTypeCategories,
        )
        val timetableData = if (includeTimetable) {
            getTimetableViewData(
                range = range,
                records = records,
                runningRecords = runningRecords,
                recordTypes = recordTypes,
                isDarkTheme = isDarkTheme,
                useMilitaryTime = useMilitaryTime,
            )
        } else {
            emptyList()
        }
        return holders + timetableData
    }

    /**
     * Timetable slots of the day as cards between the records: a slot
     * counts as attended when a record of the linked activity overlaps
     * the slot; slots in the past without a record show as missed.
     */
    private suspend fun getTimetableViewData(
        range: Range,
        records: List<Record>,
        runningRecords: List<RunningRecord>,
        recordTypes: Map<Long, RecordType>,
        isDarkTheme: Boolean,
        useMilitaryTime: Boolean,
    ): List<RecordHolder<Data>> {
        val dayTimestamp = range.timeStarted + startOfDayShiftSafe()
        val date = timetableViewDataMapper.dateString(dayTimestamp)
        if (timetableRepo.getDays().any { it.date == date && it.freeDay }) return emptyList()

        val isoDay = timetableViewDataMapper.isoDayOfWeek(dayTimestamp)
        val overrides = timetableRepo.getOverrides(date).associateBy { it.eventId }
        val midnight = timetableViewDataMapper.midnightOf(dayTimestamp)
        val now = System.currentTimeMillis()

        return timetableRepo.getEvents(isoDay).mapNotNull { event ->
            val override = overrides[event.id]
            if (override?.cancelled == true) return@mapNotNull null
            val startTime = override?.startTime ?: event.startTime
            val endTime = override?.endTime ?: event.endTime
            val slotStart = midnight + startTime * minuteInMillis
            val slotEnd = midnight + endTime * minuteInMillis
            val typeId = event.activityTypeId

            val attended = typeId != null && (
                records.any { record ->
                    record.typeId == typeId &&
                        record.timeStarted < slotEnd &&
                        record.timeEnded > slotStart
                } ||
                    runningRecords.any { running ->
                        running.id == typeId && running.timeStarted < slotEnd
                    }
                )

            val color = typeId?.let { recordTypes[it] }?.color
                ?.let { colorMapper.mapToColorInt(it, isDarkTheme) }
                ?: colorMapper.mapToColorInt(
                    com.example.util.simpletimetracker.domain.color.model.AppColor(
                        colorId = 10,
                        colorInt = "",
                    ),
                    isDarkTheme,
                )

            RecordHolder<Data>(
                timeStartedTimestamp = slotStart,
                typeId = typeId ?: 0L,
                tagIds = emptyList(),
                data = Data.TimetableEventData(
                    timetableViewDataMapper.map(
                        eventId = event.id,
                        name = event.name,
                        room = override?.room ?: event.room,
                        comment = event.comment,
                        slotStart = slotStart,
                        slotEnd = slotEnd,
                        attended = attended,
                        color = color,
                        useMilitaryTime = useMilitaryTime,
                        now = now,
                    ),
                ),
            )
        }
    }

    private suspend fun startOfDayShiftSafe(): Long {
        return prefsInteractor.getStartOfDayShift()
    }

    /**
     * Timetable slots of the day as translucent background bands for the
     * calendar view; free days have no slots, overrides move or cancel
     * single slots. Start and end are milliseconds from day start.
     */
    private suspend fun getTimetableSlots(
        range: Range,
        records: List<Record>,
        runningRecords: List<RunningRecord>,
        recordTypes: Map<Long, RecordType>,
        isDarkTheme: Boolean,
    ): List<RecordsCalendarViewData.Slot> {
        val dayTimestamp = range.timeStarted + startOfDayShiftSafe()
        val date = timetableViewDataMapper.dateString(dayTimestamp)
        if (timetableRepo.getDays().any { it.date == date && it.freeDay }) return emptyList()

        val isoDay = timetableViewDataMapper.isoDayOfWeek(dayTimestamp)
        val overrides = timetableRepo.getOverrides(date).associateBy { it.eventId }
        val midnight = timetableViewDataMapper.midnightOf(dayTimestamp)
        val startOfDayShift = prefsInteractor.getStartOfDayShift()

        return timetableRepo.getEvents(isoDay).mapNotNull { event ->
            val override = overrides[event.id]
            if (override?.cancelled == true) return@mapNotNull null
            val startTime = override?.startTime ?: event.startTime
            val endTime = override?.endTime ?: event.endTime

            val color = event.activityTypeId
                ?.let { recordTypes[it] }?.color
                ?.let { colorMapper.mapToColorInt(it, isDarkTheme) }
                ?: colorMapper.mapToColorInt(
                    com.example.util.simpletimetracker.domain.color.model.AppColor(
                        colorId = 10,
                        colorInt = "",
                    ),
                    isDarkTheme,
                )

            // Absolute timestamps for the attendance check.
            val slotStartAbs = midnight + startTime * minuteInMillis
            val slotEndAbs = midnight + endTime * minuteInMillis
            val attended = event.activityTypeId != null && (
                records.any { record ->
                    record.typeId == event.activityTypeId &&
                        record.timeStarted < slotEndAbs &&
                        record.timeEnded > slotStartAbs
                } ||
                    runningRecords.any { running ->
                        running.id == event.activityTypeId &&
                            running.timeStarted < slotEndAbs
                    }
                )
            val state = when {
                attended -> RecordsCalendarViewData.Slot.STATE_ATTENDED
                System.currentTimeMillis() > slotEndAbs ->
                    RecordsCalendarViewData.Slot.STATE_MISSED
                else -> RecordsCalendarViewData.Slot.STATE_UPCOMING
            }

            // Window coordinates: the chart measures from the shifted day
            // start, so convert the wall clock minutes accordingly.
            val startCoord = (
                (startTime * minuteInMillis - startOfDayShift + dayInMillis) % dayInMillis
                )
            RecordsCalendarViewData.Slot(
                start = startCoord,
                end = startCoord + (endTime - startTime) * minuteInMillis,
                color = color,
                name = event.name,
                typeLabel = when (event.type) {
                    TimetableEvent.Type.LECTURE -> "V"
                    TimetableEvent.Type.EXERCISE -> "UE"
                    TimetableEvent.Type.TUTORIUM -> "T"
                },
                state = state,
                time = timetableViewDataMapper.formatRange(
                    start = slotStartAbs,
                    end = slotEndAbs,
                    useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat(),
                ),
                room = override?.room ?: event.room,
                comment = event.comment,
                startTimestamp = slotStartAbs,
                endTimestamp = slotEndAbs,
                activityTypeId = event.activityTypeId,
                eventId = event.id,
            )
        }
    }

    private fun mapToCalendarPoint(
        holder: RecordHolder<Data>,
        calendar: Calendar,
        startOfDayShift: Long,
        rangeStart: Long,
        rangeEnd: Long,
        showSeconds: Boolean,
        multiSelectedIds: List<MultiSelectedRecordId>,
    ): RecordsCalendarViewData.Point {
        // Record data already clamped.
        val timeStartedTimestamp = when (holder.data) {
            is Data.RecordData ->
                holder.timeStartedTimestamp.let { if (showSeconds) it else it.dropSeconds() }
            is Data.RunningRecordData ->
                max(holder.timeStartedTimestamp, rangeStart)
            // Timetable slots are never part of the calendar view data.
            is Data.TimetableEventData ->
                throw IllegalStateException("Timetable slots are not mapped to calendar points")
        }
        val timeEndedTimestamp = when (val data = holder.data) {
            is Data.RecordData ->
                data.value.timeEndedTimestamp.let { if (showSeconds) it else it.dropSeconds() }
            is Data.RunningRecordData ->
                min(System.currentTimeMillis(), rangeEnd)
            is Data.TimetableEventData ->
                throw IllegalStateException("Timetable slots are not mapped to calendar points")
        }

        val start = timeMapper.mapFromStartOfDay(
            // Normalize to set start of day correctly.
            timeStamp = timeStartedTimestamp - startOfDayShift,
            calendar = calendar,
        ) + startOfDayShift

        val duration = (timeEndedTimestamp - timeStartedTimestamp)
            // Otherwise would be invisible.
            .takeUnless { it == 0L } ?: minuteInMillis

        val end = start + duration

        val isSelected = mapMultiSelectedId(holder) in multiSelectedIds

        return RecordsCalendarViewData.Point(
            start = start - startOfDayShift,
            end = end - startOfDayShift,
            isSelected = isSelected,
            data = when (val data = holder.data) {
                is Data.RecordData -> {
                    RecordsCalendarViewData.Point.Data.RecordData(data.value)
                }
                is Data.RunningRecordData -> {
                    RecordsCalendarViewData.Point.Data.RunningRecordData(data.value)
                }
                is Data.TimetableEventData ->
                    throw IllegalStateException("Timetable slots are not mapped to calendar points")
            },
        )
    }

    private fun mapMultiSelectedId(
        holder: RecordHolder<Data>,
    ): MultiSelectedRecordId {
        return when (val data = holder.data) {
            is Data.RecordData -> {
                when (val value = data.value) {
                    is RecordViewData.Tracked -> MultiSelectedRecordId.Tracked(value.id)
                    is RecordViewData.Untracked -> MultiSelectedRecordId.Untracked(
                        timeStartedTimestamp = value.timeStartedTimestamp,
                        timeEndedTimestamp = value.timeEndedTimestamp,
                    )
                }
            }
            is Data.RunningRecordData -> {
                val value = data.value
                MultiSelectedRecordId.Running(value.id)
            }
            // Timetable cards do not take part in multiselection.
            is Data.TimetableEventData ->
                throw IllegalStateException("Timetable slots cannot be multiselected")
        }
    }

    private sealed interface Data {
        val value: ViewHolderType

        data class RecordData(
            override val value: RecordViewData,
        ) : Data

        data class RunningRecordData(
            override val value: RunningRecordViewData,
        ) : Data

        data class TimetableEventData(
            override val value: TimetableViewData,
        ) : Data
    }

    private data class ViewDataIntermediate(
        val rangeStart: Long,
        val rangeEnd: Long,
        val isToday: Boolean,
        val records: List<RecordHolder<Data>>,
        val slots: List<RecordsCalendarViewData.Slot> = emptyList(),
    )

    companion object {
        private val minuteInMillis = TimeUnit.MINUTES.toMillis(1)
        private val dayInMillis = TimeUnit.DAYS.toMillis(1)
    }
}