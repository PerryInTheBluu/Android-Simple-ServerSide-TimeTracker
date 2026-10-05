package com.example.util.simpletimetracker.feature_widget.common

import android.content.Context
import com.example.util.simpletimetracker.core.extension.toParams
import com.example.util.simpletimetracker.core.interactor.CompleteTypesStateInteractor
import com.example.util.simpletimetracker.core.interactor.RecordRepeatInteractor
import com.example.util.simpletimetracker.domain.base.REPEAT_BUTTON_ITEM_ID
import com.example.util.simpletimetracker.domain.record.interactor.AddRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RemoveRunningRecordMediator
import com.example.util.simpletimetracker.domain.record.interactor.RunningRecordInteractor
import com.example.util.simpletimetracker.domain.record.model.RecordBase
import com.example.util.simpletimetracker.domain.record.model.RecordDataSelectionDialogResult
import com.example.util.simpletimetracker.domain.recordType.interactor.RecordTypeInteractor
import com.example.util.simpletimetracker.feature_widget.single.WidgetSingleTagSelectionActivity
import com.example.util.simpletimetracker.navigation.params.screen.RecordTagSelectionParams
import kotlinx.coroutines.delay
import javax.inject.Inject

class WidgetTypeClickManager @Inject constructor(
    private val recordRepeatInteractor: RecordRepeatInteractor,
    private val recordTypeInteractor: RecordTypeInteractor,
    private val completeTypesStateInteractor: CompleteTypesStateInteractor,
    private val runningRecordInteractor: RunningRecordInteractor,
    private val addRunningRecordMediator: AddRunningRecordMediator,
    private val removeRunningRecordMediator: RemoveRunningRecordMediator,
) {

    suspend fun onClick(
        context: Context?,
        recordTypeId: Long,
        onWidgetUpdate: () -> Unit,
    ) {
        if (recordTypeId == REPEAT_BUTTON_ITEM_ID) {
            recordRepeatInteractor.repeatForQuickTileExternal()
            return
        }

        val type = recordTypeInteractor.get(recordTypeId)

        // If recordType removed - update widget and exit
        if (type == null) {
            onWidgetUpdate()
            return
        }

        if (type.defaultDuration > 0) {
            completeTypesStateInteractor.widgetTypeIds += recordTypeId
            onWidgetUpdate()
            delay(1000)
            completeTypesStateInteractor.widgetTypeIds -= recordTypeId
            onWidgetUpdate()
        }

        val runningRecord = runningRecordInteractor.get(recordTypeId)
        if (runningRecord != null) {
            // Stop running record, add new record
            removeRunningRecordMediator.removeWithRecordAdd(runningRecord)
        } else {
            // Start running record
            addRunningRecordMediator.tryStartTimer(
                typeId = recordTypeId,
                onNeedToShowTagSelection = {
                    showTagSelection(context, recordTypeId, it)
                },
            )
        }
    }

    private fun showTagSelection(
        context: Context?,
        typeId: Long,
        result: RecordDataSelectionDialogResult,
    ) {
        context ?: return

        WidgetSingleTagSelectionActivity.getStartIntent(
            context = context,
            data = RecordTagSelectionParams(
                typeId = typeId,
                fields = result.fields.toParams(),
                preselectedTags = result.preselectedTags.map(RecordBase.Tag::toParams),
                requiredValueSelectionTagIds = result.requiredValueSelectionTagIds,
            ),
        ).let(context::startActivity)
    }
}