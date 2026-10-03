package com.example.util.simpletimetracker.feature_records.customView

import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.record.RecordViewData
import com.example.util.simpletimetracker.feature_base_adapter.runningRecord.RunningRecordViewData
import com.example.util.simpletimetracker.feature_views.viewData.RecordTypeIcon

data class RecordsCalendarViewData(
    val currentTime: Long?,
    val startOfDayShift: Long,
    // Length of the visible day window in milliseconds, measured from
    // the shifted day start; 0 means a full day.
    val endOfDayShift: Long = 0L,
    val points: List<Points>,
    val reverseOrder: Boolean,
    val shouldDrawTopLegends: Boolean,
    val isMilitary: Boolean,
) {

    data class Points(
        val legend: String,
        val highlighted: Boolean,
        val data: List<Point>,
        // Timetable slots of the day drawn as background bands behind the
        // record bars; start and end are milliseconds from day start.
        val slots: List<Slot> = emptyList(),
    )

    data class Slot(
        val start: Long,
        val end: Long,
        val color: Int,
        val name: String,
        // Short type label like "V", "UE" or "T".
        val typeLabel: String,
        // ATTENDED, MISSED or UPCOMING.
        val state: Int,
    ) {
        companion object {
            const val STATE_UPCOMING = 0
            const val STATE_ATTENDED = 1
            const val STATE_MISSED = 2
        }
    }

    data class Point(
        val start: Long,
        val end: Long,
        val isSelected: Boolean,
        val data: Data,
    ) {

        sealed interface Data {
            val value: ViewHolderType
            val color: Int
            val duration: String
            val name: String
            val tagName: String
            val iconId: RecordTypeIcon
            val comment: String

            data class RecordData(override val value: RecordViewData) : Data {
                override val color = value.color
                override val duration = value.duration
                override val name = value.name
                override val tagName = value.tagName
                override val iconId = value.iconId
                override val comment = value.comment
            }

            data class RunningRecordData(override val value: RunningRecordViewData) : Data {
                override val color = value.color
                override val duration = value.timer
                override val name = value.name
                override val tagName = value.tagName
                override val iconId = value.iconId
                override val comment = value.comment
            }
        }
    }
}