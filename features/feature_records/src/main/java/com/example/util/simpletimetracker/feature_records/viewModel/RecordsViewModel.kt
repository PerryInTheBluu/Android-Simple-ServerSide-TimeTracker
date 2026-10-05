package com.example.util.simpletimetracker.feature_records.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.util.simpletimetracker.core.base.BaseViewModel
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.base.SingleLiveEvent
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.extension.toParams
import com.example.util.simpletimetracker.core.interactor.GetChangeRecordNavigationParamsInteractor
import com.example.util.simpletimetracker.core.interactor.SharingInteractor
import com.example.util.simpletimetracker.core.mapper.RangeViewDataMapper
import com.example.util.simpletimetracker.core.model.NavigationTab
import com.example.util.simpletimetracker.domain.darkMode.interactor.ThemeChangedInteractor
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordsContainerMultiselectInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordsShareUpdateInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordsUpdateInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.notifications.interactor.UpdateExternalViewsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.UpdateRunningRecordsInteractor
import com.example.util.simpletimetracker.domain.record.model.MultiSelectedRecordId
import com.example.util.simpletimetracker.domain.statistics.model.ChartFilterType
import com.example.util.simpletimetracker.domain.statistics.model.RangeLength
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.loader.LoaderViewData
import com.example.util.simpletimetracker.feature_base_adapter.record.RecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.runningRecord.RunningRecordViewData
import com.example.util.simpletimetracker.feature_records.extra.RecordsExtra
import com.example.util.simpletimetracker.feature_records.interactor.RecordsViewDataInteractor
import com.example.util.simpletimetracker.feature_records.mapper.RecordsViewDataMapper
import com.example.util.simpletimetracker.feature_records.model.RecordsShareState
import com.example.util.simpletimetracker.feature_records.model.TimetableAction
import com.example.util.simpletimetracker.domain.timetable.model.TimetableDay
import com.example.util.simpletimetracker.domain.timetable.model.TimetableEventOverride
import com.example.util.simpletimetracker.domain.timetable.notification.TimetableNotificationInteractor
import com.example.util.simpletimetracker.domain.timetable.repo.TimetableRepo
import com.example.util.simpletimetracker.domain.notifications.interactor.LocalDataChangedBus
import com.example.util.simpletimetracker.navigation.params.screen.OptionsListParams
import com.example.util.simpletimetracker.navigation.params.screen.TextInputDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.DateTimeDialogParams
import com.example.util.simpletimetracker.navigation.params.screen.DateTimeDialogType
import java.time.ZoneId
import java.time.LocalDate
import com.example.util.simpletimetracker.feature_records.R
import com.example.util.simpletimetracker.navigation.params.screen.TimetableSlotDialogParams
import com.example.util.simpletimetracker.feature_records.customView.RecordsCalendarViewData
import com.example.util.simpletimetracker.feature_records.model.RecordsState
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRecordFromMainParams
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRecordParams
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRunningRecordFromMainParams
import com.example.util.simpletimetracker.navigation.params.screen.ChangeRunningRecordParams
import com.example.util.simpletimetracker.navigation.params.screen.RecordQuickActionsParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val router: Router,
    private val recordsViewDataInteractor: RecordsViewDataInteractor,
    private val prefsInteractor: PrefsInteractor,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val recordInteractor: RecordInteractor,
    private val updateExternalViewsInteractor: UpdateExternalViewsInteractor,
    private val recordsUpdateInteractor: RecordsUpdateInteractor,
    private val recordsShareUpdateInteractor: RecordsShareUpdateInteractor,
    private val sharingInteractor: SharingInteractor,
    private val rangeViewDataMapper: RangeViewDataMapper,
    private val recordsViewDataMapper: RecordsViewDataMapper,
    private val updateRunningRecordsInteractor: UpdateRunningRecordsInteractor,
    private val getChangeRecordNavigationParamsInteractor: GetChangeRecordNavigationParamsInteractor,
    private val recordsContainerMultiselectInteractor: RecordsContainerMultiselectInteractor,
    private val themeChangedInteractor: ThemeChangedInteractor,
    private val timetableRepo: TimetableRepo,
    private val timetableNotificationInteractor: TimetableNotificationInteractor,
) : BaseViewModel() {

    override var delayDataLoad: Boolean = false

    var extra: RecordsExtra? = null

    val isCalendarView: LiveData<Boolean> = MutableLiveData()
    val records: LiveData<List<ViewHolderType>> by lazy {
        MutableLiveData(listOf(LoaderViewData() as ViewHolderType))
    }
    val calendarData: LiveData<RecordsState.CalendarData> by lazy {
        MutableLiveData(RecordsState.CalendarData.Loading)
    }
    val sharingData: SingleLiveEvent<RecordsShareState> = SingleLiveEvent()
    val resetScreen: SingleLiveEvent<Unit> = SingleLiveEvent()
    val previewUpdate: SingleLiveEvent<UpdateRunningRecordsInteractor.Update> = SingleLiveEvent()

    private var isVisible: Boolean = false
    private var isCalendarMode: Boolean = false
    private var timerJob: Job? = null
    private var pendingTimeChange: Triple<Long, String, Int>? = null
    private var updateJob: Job? = null
    private val shift: Int get() = extra?.shift.orZero()

    init {
        subscribeToUpdates()
    }

    fun onCalendarClick(item: ViewHolderType) {
        when (item) {
            is RecordViewData -> onRecordClick(item)
            is RunningRecordViewData -> onRunningRecordClick(item)
        }
    }

    fun onTimetableSlotClick(slot: RecordsCalendarViewData.Slot) {
        viewModelScope.launch {
            val typeLabel = when (slot.typeLabel) {
                "V" -> resourceRepo.getString(R.string.timetable_type_lecture)
                "UE" -> resourceRepo.getString(R.string.timetable_type_exercise)
                else -> resourceRepo.getString(R.string.timetable_type_tutorium)
            }
            val stateText = when (slot.state) {
                RecordsCalendarViewData.Slot.STATE_ATTENDED ->
                    resourceRepo.getString(R.string.timetable_state_attended)
                RecordsCalendarViewData.Slot.STATE_MISSED ->
                    resourceRepo.getString(R.string.timetable_state_missed)
                else -> resourceRepo.getString(R.string.timetable_state_upcoming)
            }
            val roomText = slot.room.takeIf { it.isNotEmpty() }
                ?.let { room -> resourceRepo.getString(R.string.timetable_dialog_room, room) }
                .orEmpty()
            val commentText = slot.comment.takeIf { it.isNotEmpty() }
                ?.let { comment -> resourceRepo.getString(R.string.timetable_dialog_comment, comment) }
                .orEmpty()
            val info = buildString {
                append(slot.time)
                append(roomText)
                append(commentText)
                append("\n\n")
                append(stateText)
            }
            val date = LocalDate.now().plusDays(shift.toLong()).toString()
            val todos = timetableRepo.getTodos(slot.eventId)
                .filter { it.date == date }
                .map { todo ->
                    TimetableSlotDialogParams.Todo(
                        id = todo.id,
                        text = todoText(todo),
                        done = todo.done,
                    )
                }
            router.navigate(
                TimetableSlotDialogParams(
                    slot = slot,
                    title = slot.name + " (" + typeLabel + ")",
                    info = info,
                    canNachtragen = slot.state == RecordsCalendarViewData.Slot.STATE_MISSED,
                    btnNachtragen = resourceRepo.getString(R.string.timetable_dialog_nachtragen),
                    todos = todos,
                ),
            )
        }
    }

    private fun todoText(todo: com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo): String {
        return when (todo.type) {
            com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo.Type.PREPARATION ->
                resourceRepo.getString(R.string.timetable_todo_preparation)
            com.example.util.simpletimetracker.domain.timetable.model.TimetableTodo.Type.FOLLOW_UP ->
                resourceRepo.getString(R.string.timetable_todo_follow_up)
            else -> todo.text
        }
    }

    fun onSlotTodoToggle(todoId: Long) = viewModelScope.launch {
        val todo = timetableRepo.getAllTodos().firstOrNull { it.id == todoId } ?: return@launch
        timetableRepo.setTodoDone(todoId, !todo.done)
        LocalDataChangedBus.publish()
        updateRecords()
    }

    fun onTimetableSlotNachtragen(slot: RecordsCalendarViewData.Slot) {
        viewModelScope.launch {
            val typeId = slot.activityTypeId ?: return@launch
            recordInteractor.add(
                Record(
                    typeId = typeId,
                    timeStarted = slot.startTimestamp,
                    timeEnded = slot.endTimestamp,
                    comment = "",
                    tags = emptyList(),
                ),
            )
            updateExternalViewsInteractor.onRecordAddOrChange(
                typeIds = listOf(typeId),
                tagIds = emptyList(),
                updateNotificationSwitch = false,
            )
            updateRecords()
        }
    }

    fun onTimetableSlotLongClick(slot: RecordsCalendarViewData.Slot) = viewModelScope.launch {
        val date = LocalDate.now().plusDays(shift.toLong()).toString()
        val hasOverride = timetableRepo.getOverrides(date).any { it.eventId == slot.eventId }
        val items = buildList {
            if (hasOverride) {
                add(
                    OptionsListParams.Item(
                        id = TimetableAction(TimetableAction.Type.RESTORE_SLOT, date, slot.eventId),
                        text = resourceRepo.getString(R.string.timetable_action_restore),
                        icon = null,
                    ),
                )
            } else {
                add(
                    OptionsListParams.Item(
                        id = TimetableAction(TimetableAction.Type.CANCEL_SLOT, date, slot.eventId),
                        text = resourceRepo.getString(R.string.timetable_action_cancel),
                        icon = null,
                    ),
                )
            }
            add(
                OptionsListParams.Item(
                    id = TimetableAction(TimetableAction.Type.CHANGE_TIME, date, slot.eventId),
                    text = resourceRepo.getString(R.string.timetable_action_change_time),
                    icon = null,
                ),
            )
            add(
                OptionsListParams.Item(
                    id = TimetableAction(TimetableAction.Type.CHANGE_ROOM, date, slot.eventId),
                    text = resourceRepo.getString(R.string.timetable_action_change_room),
                    icon = null,
                ),
            )
        }
        router.navigate(OptionsListParams(items))
    }

    fun onCalendarEmptyLongPress() = viewModelScope.launch {
        val date = LocalDate.now().plusDays(shift.toLong()).toString()
        val freeDay = timetableRepo.getDays().firstOrNull { it.date == date && it.freeDay }
        val dayItem = OptionsListParams.Item(
            id = if (freeDay != null) {
                TimetableAction(TimetableAction.Type.REMOVE_FREE_DAY, date)
            } else {
                TimetableAction(TimetableAction.Type.ADD_FREE_DAY, date)
            },
            text = resourceRepo.getString(
                if (freeDay != null) {
                    R.string.timetable_action_remove_free_day
                } else {
                    R.string.timetable_action_free_day
                },
            ),
            icon = null,
        )
        // Cancelled slots are not rendered, so their exceptions are
        // reachable from the day menu.
        val events = timetableRepo.getAllEvents().associateBy { it.id }
        val restoreItems = timetableRepo.getOverrides(date)
            .filter { events[it.eventId] != null }
            .map { override ->
                OptionsListParams.Item(
                    id = TimetableAction(TimetableAction.Type.RESTORE_SLOT, date, override.eventId),
                    text = resourceRepo.getString(
                        R.string.timetable_action_restore_slot,
                        events[override.eventId]?.name.orEmpty(),
                    ),
                    icon = null,
                )
            }
        router.navigate(OptionsListParams(listOf(dayItem) + restoreItems))
    }

    fun onTimetableAction(action: TimetableAction) = viewModelScope.launch {
        when (action.action) {
            TimetableAction.Type.CANCEL_SLOT -> {
                timetableRepo.addOverride(
                    TimetableEventOverride(
                        date = action.date,
                        eventId = action.eventId,
                        room = "",
                        startTime = 0,
                        endTime = 0,
                        cancelled = true,
                        note = "",
                    ),
                )
            }
            TimetableAction.Type.RESTORE_SLOT -> {
                timetableRepo.getOverrides(action.date)
                    .firstOrNull { it.eventId == action.eventId }
                    ?.let { timetableRepo.removeOverride(it.id) }
            }
            TimetableAction.Type.CHANGE_TIME -> openSlotTimeDialog(
                eventId = action.eventId,
                date = action.date,
                isStart = true,
            )
            TimetableAction.Type.CHANGE_ROOM -> {
                val event = timetableRepo.getAllEvents().firstOrNull { it.id == action.eventId }
                val currentRoom = timetableRepo.getOverrides(action.date)
                    .firstOrNull { it.eventId == action.eventId }
                    ?.room?.takeIf { it.isNotEmpty() }
                    ?: event?.room.orEmpty()
                router.navigate(
                    TextInputDialogParams(
                        tag = TAG_ROOM + action.eventId + "_" + action.date,
                        title = resourceRepo.getString(R.string.timetable_action_change_room),
                        prefill = currentRoom,
                        hint = resourceRepo.getString(R.string.timetable_action_change_room_hint),
                    ),
                )
            }
            TimetableAction.Type.ADD_FREE_DAY -> {
                val exists = timetableRepo.getDays().any { it.date == action.date && it.freeDay }
                if (!exists) {
                    timetableRepo.addDay(
                        TimetableDay(date = action.date, freeDay = true, note = ""),
                    )
                }
            }
            TimetableAction.Type.REMOVE_FREE_DAY -> {
                timetableRepo.getDays()
                    .firstOrNull { it.date == action.date && it.freeDay }
                    ?.let { timetableRepo.removeDay(it.id) }
            }
        }
        timetableNotificationInteractor.rescheduleAll()
        LocalDataChangedBus.publish()
        updateRecords()
    }

    fun onSlotTimeSet(timestamp: Long, tag: String?) = viewModelScope.launch {
        if (tag.orEmpty().startsWith(TAG_TIME_START)) {
            val (eventId, date) = parseSlotTag(tag.orEmpty(), TAG_TIME_START)
            pendingTimeChange = Triple(eventId, date, minutesOfDay(timestamp))
            openSlotTimeDialog(eventId, date, isStart = false)
        } else if (tag.orEmpty().startsWith(TAG_TIME_END)) {
            val (eventId, date) = parseSlotTag(tag.orEmpty(), TAG_TIME_END)
            val pending = pendingTimeChange
            if (pending != null && pending.first == eventId && pending.second == date) {
                setSlotOverride(eventId, date, startTime = pending.third, endTime = minutesOfDay(timestamp))
                pendingTimeChange = null
            }
        }
    }

    fun onSlotRoomInput(room: String, tag: String?) = viewModelScope.launch {
        if (!tag.orEmpty().startsWith(TAG_ROOM)) return@launch
        val (eventId, date) = parseSlotTag(tag.orEmpty(), TAG_ROOM)
        setSlotOverride(eventId, date, room = room)
    }

    // Replaces the existing exception of the slot with a merged one, so
    // room and time changes do not wipe each other.
    private suspend fun setSlotOverride(
        eventId: Long,
        date: String,
        room: String? = null,
        startTime: Int? = null,
        endTime: Int? = null,
    ) {
        val event = timetableRepo.getAllEvents().firstOrNull { it.id == eventId } ?: return
        val existing = timetableRepo.getOverrides(date).firstOrNull { it.eventId == eventId }
        val newOverride = TimetableEventOverride(
            date = date,
            eventId = eventId,
            room = room ?: existing?.room.takeIf { it?.isNotEmpty() == true } ?: "",
            startTime = startTime ?: existing?.startTime ?: 0,
            endTime = endTime ?: existing?.endTime ?: 0,
            cancelled = false,
            note = "",
        )
        if (existing != null) {
            timetableRepo.removeOverride(existing.id)
        }
        // An override without room, time or cancel information would only
        // shadow the event; do not keep it.
        val carriesInformation = newOverride.room.isNotEmpty() ||
            newOverride.startTime != 0 ||
            newOverride.endTime != 0
        if (carriesInformation) {
            timetableRepo.addOverride(newOverride)
        }
        timetableNotificationInteractor.rescheduleAll()
        LocalDataChangedBus.publish()
        updateRecords()
    }

    private suspend fun openSlotTimeDialog(
        eventId: Long,
        date: String,
        isStart: Boolean,
    ) {
        val event = timetableRepo.getAllEvents().firstOrNull { it.id == eventId } ?: return
        val existing = timetableRepo.getOverrides(date).firstOrNull { it.eventId == eventId }
        val minutes = if (isStart) {
            existing?.startTime?.takeIf { it != 0 } ?: event.startTime
        } else {
            pendingTimeChange?.third?.plus(60)
                ?: (existing?.endTime?.takeIf { it != 0 } ?: event.endTime)
        }
        val base = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val tag = (if (isStart) TAG_TIME_START else TAG_TIME_END) + eventId + "_" + date
        router.navigate(
            DateTimeDialogParams(
                tag = tag,
                type = DateTimeDialogType.TIME,
                timestamp = base + minutes * 60_000L,
                useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat(),
            ),
        )
    }

    private fun parseSlotTag(tag: String, prefix: String): Pair<Long, String> {
        val payload = tag.removePrefix(prefix)
        val parts = payload.split("_")
        return (parts.getOrNull(0)?.toLongOrNull() ?: 0L) to (parts.getOrNull(1) ?: "")
    }

    private fun minutesOfDay(timestamp: Long): Int {
        val time = java.time.Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalTime()
        return time.hour * 60 + time.minute
    }

    fun onCalendarLongClick(item: ViewHolderType) {
        when (item) {
            is RecordViewData -> onRecordLongClick(item)
            is RunningRecordViewData -> onRunningRecordLongClick(item)
        }
    }

    fun onRunningRecordClick(
        item: RunningRecordViewData,
        sharedElements: Pair<Any, String>? = null,
    ) = viewModelScope.launch {
        if (runningRecordInteractor.get(item.id) == null) return@launch
        if (recordsContainerMultiselectInteractor.isEnabled) {
            onMultiselectRunningRecordClick(item)
            return@launch
        }
        val useMilitaryTimeFormat = prefsInteractor.getUseMilitaryTimeFormat()
        val showSeconds = prefsInteractor.getShowSeconds()
        val durationFormat = prefsInteractor.getDurationFormat()
        val params = getChangeRecordNavigationParamsInteractor.execute(
            item = item,
            from = ChangeRunningRecordParams.From.Records,
            useMilitaryTimeFormat = useMilitaryTimeFormat,
            showSeconds = showSeconds,
            durationFormat = durationFormat,
            sharedElements = sharedElements,
        )
        throttle {
            if (sharedElements == null) delayDataLoad = true
            router.navigate(
                data = ChangeRunningRecordFromMainParams(params),
                sharedElements = sharedElements?.let(::mapOf).orEmpty(),
            )
        }.invoke()
    }

    fun onRecordClick(
        item: RecordViewData,
        sharedElements: Pair<Any, String>? = null,
    ) = viewModelScope.launch {
        if (recordsContainerMultiselectInteractor.isEnabled) {
            onMultiselectRecordClick(item)
            return@launch
        }
        val useMilitaryTimeFormat = prefsInteractor.getUseMilitaryTimeFormat()
        val showSeconds = prefsInteractor.getShowSeconds()
        val durationFormat = prefsInteractor.getDurationFormat()
        val params = getChangeRecordNavigationParamsInteractor.execute(
            item = item,
            from = ChangeRecordParams.From.Records,
            shift = shift,
            useMilitaryTimeFormat = useMilitaryTimeFormat,
            showSeconds = showSeconds,
            durationFormat = durationFormat,
            sharedElements = sharedElements,
        )
        throttle {
            if (sharedElements == null) delayDataLoad = true
            router.navigate(
                data = ChangeRecordFromMainParams(params),
                sharedElements = sharedElements?.let(::mapOf).orEmpty(),
            )
        }.invoke()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRunningRecordLongClick(
        item: RunningRecordViewData,
        sharedElements: Pair<Any, String>? = null,
    ) {
        if (recordsContainerMultiselectInteractor.isEnabled) {
            val id = MultiSelectedRecordId.Running(item.id)
            if (id !in recordsContainerMultiselectInteractor.selectedRecordIds) {
                recordsContainerMultiselectInteractor.onRecordClick(id)
                updateRecords()
                return
            }
        }
        val navParams = RecordQuickActionsParams(
            type = RecordQuickActionsParams.Type.RecordRunning(
                id = item.id,
            ),
            preview = RecordQuickActionsParams.Preview(
                name = item.name,
                iconId = item.iconId.toParams(),
                color = item.color,
            ),
        )
        throttle {
            router.navigate(navParams)
        }.invoke()
    }

    @Suppress("UNUSED_PARAMETER")
    fun onRecordLongClick(
        item: RecordViewData,
        sharedElements: Pair<Any, String>? = null,
    ) {
        if (recordsContainerMultiselectInteractor.isEnabled) {
            val id = when (item) {
                is RecordViewData.Tracked -> MultiSelectedRecordId.Tracked(item.id)
                is RecordViewData.Untracked -> MultiSelectedRecordId.Untracked(
                    timeStartedTimestamp = item.timeStartedTimestamp,
                    timeEndedTimestamp = item.timeEndedTimestamp,
                )
            }
            if (id !in recordsContainerMultiselectInteractor.selectedRecordIds) {
                recordsContainerMultiselectInteractor.onRecordClick(id)
                updateRecords()
                return
            }
        }
        val type = when (item) {
            is RecordViewData.Tracked -> RecordQuickActionsParams.Type.RecordTracked(
                id = item.id,
            )
            is RecordViewData.Untracked -> RecordQuickActionsParams.Type.RecordUntracked(
                timeStarted = item.timeStartedTimestamp,
                timeEnded = item.timeEndedTimestamp,
            )
        }
        val navParams = RecordQuickActionsParams(
            type = type,
            preview = RecordQuickActionsParams.Preview(
                name = item.name,
                iconId = item.iconId.toParams(),
                color = item.color,
            ),
        )
        throttle {
            router.navigate(navParams)
        }.invoke()
    }

    fun onVisible() {
        isVisible = true
        startUpdate()
    }

    fun onHidden() {
        isVisible = false
        stopUpdate()
    }

    fun onNeedUpdate() {
        if (isVisible) updateRecords()
    }

    fun onTabReselected(tab: NavigationTab?) {
        if (isVisible && tab is NavigationTab.Records) {
            resetScreen.set(Unit)
        }
    }

    fun onShareView(view: Any) = viewModelScope.launch {
        sharingInteractor.execute(view = view, filename = SHARING_NAME)
    }

    fun onFilterApplied(
        chartFilterType: ChartFilterType,
        dataIds: List<Long>,
    ) = viewModelScope.launch {
        if (!isVisible) return@launch
        prefsInteractor.setListFilterType(chartFilterType)
        when (chartFilterType) {
            ChartFilterType.ACTIVITY -> prefsInteractor.setFilteredTypesOnList(dataIds)
            ChartFilterType.CATEGORY -> prefsInteractor.setFilteredCategoriesOnList(dataIds)
            ChartFilterType.RECORD_TAG -> prefsInteractor.setFilteredTagsOnList(dataIds)
        }
    }

    private fun subscribeToUpdates() {
        viewModelScope.launch {
            recordsUpdateInteractor.dataUpdated.collect { if (isVisible) updateRecords() }
        }
        viewModelScope.launch {
            recordsShareUpdateInteractor.shareClicked.collect { if (isVisible) onShareClicked() }
        }
        viewModelScope.launch {
            updateRunningRecordsInteractor.dataUpdated.collect { onUpdateReceived(it) }
        }
        viewModelScope.launch {
            themeChangedInteractor.themeChanged.collect { updateRecords() }
        }
    }

    private fun onUpdateReceived(
        update: UpdateRunningRecordsInteractor.Update,
    ) {
        // No need to update.
        if (shift != 0) return

        previewUpdate.set(update)
    }

    private fun onMultiselectRunningRecordClick(item: RunningRecordViewData) {
        val id = MultiSelectedRecordId.Running(item.id)
        recordsContainerMultiselectInteractor.onRecordClick(id)
        updateRecords()
    }

    private fun onMultiselectRecordClick(item: RecordViewData) {
        val id = when (item) {
            is RecordViewData.Tracked -> MultiSelectedRecordId.Tracked(item.id)
            is RecordViewData.Untracked -> MultiSelectedRecordId.Untracked(
                timeStartedTimestamp = item.timeStartedTimestamp,
                timeEndedTimestamp = item.timeEndedTimestamp,
            )
        }
        recordsContainerMultiselectInteractor.onRecordClick(id)
        updateRecords()
    }

    private suspend fun onShareClicked() {
        val state = loadRecordsViewData(true)
        val data = when (state) {
            is RecordsState.RecordsData -> {
                RecordsShareState(
                    rangeViewDataMapper.mapToShareTitle(
                        rangeLength = RangeLength.Day,
                        position = shift,
                        startOfDayShift = prefsInteractor.getStartOfDayShift(),
                        firstDayOfWeek = prefsInteractor.getFirstDayOfWeek(),
                    ),
                    RecordsShareState.State.Records(state.data),
                )
            }
            is RecordsState.CalendarData.Data -> {
                RecordsShareState(
                    recordsViewDataMapper.mapToShareCalendarTitle(
                        shift = shift,
                        startOfDayShift = prefsInteractor.getStartOfDayShift(),
                        isCalendarView = prefsInteractor.getShowRecordsCalendar(),
                        daysInCalendar = prefsInteractor.getDaysInCalendar(),
                        firstDayOfWeek = prefsInteractor.getFirstDayOfWeek(),
                    ),
                    RecordsShareState.State.Calendar(state.data),
                )
            }
            else -> return
        }
        sharingData.set(data)
    }

    private fun updateRecords() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            isCalendarMode = prefsInteractor.getShowRecordsCalendar()
            isCalendarView.set(isCalendarMode)

            when (val state = loadRecordsViewData()) {
                is RecordsState.RecordsData -> records.set(state.data)
                is RecordsState.CalendarData -> calendarData.set(state)
            }
        }
    }

    private suspend fun loadRecordsViewData(forSharing: Boolean = false): RecordsState {
        return recordsViewDataInteractor.getViewData(
            shift = shift,
            forSharing = forSharing,
        )
    }

    private fun startUpdate() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            delayLoad()
            if (shift != 0) {
                updateRecords()
                return@launch
            }
            var lastMinute = -1L
            while (isActive) {
                // Just in case update takes longer than timer period,
                // otherwise will be canceled every tick.
                if (updateJob?.isCompleted != false) {
                    // The calendar has nothing that changes per second;
                    // its current time line only moves once a minute,
                    // so a full rebuild is only needed on a minute change.
                    val minute = System.currentTimeMillis() / MILLIS_PER_MINUTE
                    if (!isCalendarMode || minute != lastMinute) {
                        lastMinute = minute
                        updateRecords()
                    }
                }
                delay(TIMER_UPDATE)
            }
        }
    }

    private fun stopUpdate() {
        timerJob?.cancel()
        updateJob?.cancel()
    }

    companion object {
        private const val TAG_ROOM = "TIMETABLE_ROOM_"
        private const val TAG_TIME_START = "TIMETABLE_TIME_START_"
        private const val TAG_TIME_END = "TIMETABLE_TIME_END_"
        private const val TIMER_UPDATE = 1000L
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val SHARING_NAME = "stt_records"
    }
}
