package com.example.util.simpletimetracker.core.interactor

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.record.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RemoveRunningRecordMediator
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.domain.record.model.Record
import com.example.util.simpletimetracker.domain.record.model.RepeatButtonType
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.domain.recordType.model.RecordType
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.notification.SnackBarParams
import com.example.util.simpletimetracker.navigation.params.notification.ToastParams
import javax.inject.Inject

// Repeats previous record, if any.
class RecordRepeatInteractor @Inject constructor(
    private val recordInteractor: RecordInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val addRunningRecordMediator: AddRunningRecordMediator,
    private val removeRunningRecordMediator: RemoveRunningRecordMediator,
    private val prefsInteractor: PrefsInteractor,
    private val router: Router,
    private val resourceRepo: ResourceRepo,
) {

    /**
     * Repeat button behaviour of the main screen and the
     * notifications: like the quick settings tile, but when nothing
     * runs the repeat button preference decides whether the last or
     * the before last finished activity starts.
     */
    suspend fun repeatButton(): ActionResult {
        return executeRepeatButtonAction { messageResId ->
            SnackBarParams(
                message = resourceRepo.getString(messageResId),
                duration = SnackBarParams.Duration.Short,
            ).let(router::show)
        }
    }

    // Can be used when app is closed (ex. from widget or notification).
    suspend fun repeatButtonExternal(): ActionResult {
        return executeRepeatButtonAction { messageResId ->
            ToastParams(
                message = resourceRepo.getString(messageResId),
            ).let(router::show)
        }
    }

    private suspend fun executeRepeatButtonAction(
        messageShower: (messageResId: Int) -> Unit,
    ): ActionResult {
        val skipLastWhenIdle = prefsInteractor.getRepeatButtonType() is RepeatButtonType.RepeatBeforeLast
        return executeStopAndStartAction(
            skipLastWhenIdle = skipLastWhenIdle,
            messageShower = messageShower,
        )
    }

    /**
     * Quick settings tile behaviour: when nothing runs, start the before
     * last finished activity (the last one is usually the break that
     * just ended); when something runs, stop it and start the last
     * finished activity (the one from before the current activity).
     */
    suspend fun repeatForQuickTileExternal() {
        executeStopAndStartAction(
            skipLastWhenIdle = true,
            messageShower = { messageResId ->
                ToastParams(
                    message = resourceRepo.getString(messageResId),
                ).let(router::show)
            },
        )
    }

    private suspend fun executeStopAndStartAction(
        skipLastWhenIdle: Boolean,
        messageShower: (messageResId: Int) -> Unit,
    ): ActionResult {
        val target = resolveRepeatTarget(skipLastWhenIdle = skipLastWhenIdle)
        if (target == null) {
            messageShower(R.string.running_records_repeat_no_prev_record)
            return ActionResult.NoPreviousFound
        }
        // Stop everything first, then start the target activity; the
        // target is resolved before stopping so the just stopped
        // records cannot shadow it.
        runningRecordInteractor.getAll().forEach {
            removeRunningRecordMediator.removeWithRecordAdd(it)
        }
        addRunningRecordMediator.startTimer(
            typeId = target.typeId,
            tags = target.tags,
            comment = target.comment,
        )
        messageShower(R.string.running_records_repeat_started)
        return ActionResult.Started
    }

    // The running activities are the most recent ones, so the target is
    // the last finished activity that none of them is tracking. With
    // several activities running (multitasking) this is the one from
    // before all of them.
    private suspend fun resolveRepeatTarget(skipLastWhenIdle: Boolean): Record? {
        val defaultTypeIds = recordTypeInteractor.getAll()
            .filter { it.defaultDuration != 0L }
            .map(RecordType::id)
        val runningTypeIds = runningRecordInteractor.getAll().map { it.id }.toSet()

        var candidate = recordInteractor.getPrev(
            timeStarted = System.currentTimeMillis(),
            ignoreTypeIds = defaultTypeIds,
        )
        if (runningTypeIds.isEmpty()) {
            // Skip the last finished activity: it is usually the break.
            if (skipLastWhenIdle) {
                candidate = candidate?.let {
                    recordInteractor.getPrev(
                        timeStarted = it.timeEnded - 1,
                        ignoreTypeIds = defaultTypeIds,
                    )
                }
            }
            return candidate
        }
        // Walk back until the activity was not running just now;
        // with one running activity this is the last finished one,
        // with several it is the one from before all of them.
        while (candidate != null && candidate.typeId in runningTypeIds) {
            candidate = recordInteractor.getPrev(
                timeStarted = candidate.timeEnded - 1,
                ignoreTypeIds = defaultTypeIds,
            )
        }
        return candidate
    }

    suspend fun repeatWithoutMessage(): ActionResult {
        return execute(messageShower = {})
    }

    private suspend fun execute(
        messageShower: (messageResId: Int) -> Unit,
    ): ActionResult {
        val type = prefsInteractor.getRepeatButtonType()
        val defaultTypeIds = recordTypeInteractor.getAll()
            .filter { it.defaultDuration != 0L }
            .map(RecordType::id)

        // TODO repeat several records?
        val prevRecord = recordInteractor.getPrev(
            timeStarted = System.currentTimeMillis(),
            ignoreTypeIds = defaultTypeIds,
        ).let {
            when (type) {
                is RepeatButtonType.RepeatLast -> it
                is RepeatButtonType.RepeatBeforeLast -> if (it != null) {
                    recordInteractor.getPrev(
                        timeStarted = it.timeEnded - 1,
                        ignoreTypeIds = defaultTypeIds,
                    )
                } else {
                    null
                }
            }
        } ?: run {
            messageShower(R.string.running_records_repeat_no_prev_record)
            return ActionResult.NoPreviousFound
        }
        if (runningRecordInteractor.get(prevRecord.typeId) != null) {
            messageShower(R.string.running_records_repeat_already_tracking)
            return ActionResult.AlreadyTracking
        }

        addRunningRecordMediator.startTimer(
            typeId = prevRecord.typeId,
            tags = prevRecord.tags,
            comment = prevRecord.comment,
        )
        return ActionResult.Started
    }

    sealed interface ActionResult {
        object Started : ActionResult
        object NoPreviousFound : ActionResult
        object AlreadyTracking : ActionResult
    }
}