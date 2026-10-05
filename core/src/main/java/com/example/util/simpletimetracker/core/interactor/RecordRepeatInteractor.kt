package com.example.util.simpletimetracker.core.interactor

import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.record.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RemoveRunningRecordMediator
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RecordInteractor
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
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

    suspend fun repeat(): ActionResult {
        return execute { messageResId ->
            SnackBarParams(
                message = resourceRepo.getString(messageResId),
                duration = SnackBarParams.Duration.Short,
            ).let(router::show)
        }
    }

    /**
     * Quick settings tile behaviour: when nothing runs, start the before
     * last finished activity (the last one is usually the break that
     * just ended); when something runs, stop it and start the last
     * finished activity (the one from before the current activity).
     */
    suspend fun repeatForQuickTileExternal() {
        executeTileAction { messageResId ->
            ToastParams(
                message = resourceRepo.getString(messageResId),
            ).let(router::show)
        }
    }

    private suspend fun executeTileAction(messageShower: (messageResId: Int) -> Unit) {
        val defaultTypeIds = recordTypeInteractor.getAll()
            .filter { it.defaultDuration != 0L }
            .map(RecordType::id)
        val running = runningRecordInteractor.getAll()
        val prev = recordInteractor.getPrev(
            timeStarted = System.currentTimeMillis(),
            ignoreTypeIds = defaultTypeIds,
        )
        // Nothing runs: the last finished activity is usually the break
        // that just ended, so continue with the one before it.
        val target = if (running.isEmpty()) {
            prev?.let {
                recordInteractor.getPrev(
                    timeStarted = it.timeEnded - 1,
                    ignoreTypeIds = defaultTypeIds,
                )
            }
        } else {
            prev
        }
        if (target == null) {
            messageShower(R.string.running_records_repeat_no_prev_record)
            return
        }
        // Something runs: stop everything first, then start the target
        // activity; stopping first would make the running one the last
        // finished record.
        running.forEach { removeRunningRecordMediator.removeWithRecordAdd(it) }
        addRunningRecordMediator.startTimer(
            typeId = target.typeId,
            tags = target.tags,
            comment = target.comment,
        )
        messageShower(R.string.running_records_repeat_started)
    }

    // Can be used than app is closed (ex. from widget).
    suspend fun repeatExternal() {
        execute { messageResId ->
            ToastParams(
                message = resourceRepo.getString(messageResId),
            ).let(router::show)
        }
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