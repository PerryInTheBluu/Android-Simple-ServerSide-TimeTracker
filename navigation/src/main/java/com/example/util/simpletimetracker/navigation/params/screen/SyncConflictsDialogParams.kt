package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SyncConflictsDialogParams(
    val dummy: Boolean = false,
) : Parcelable, ScreenParams {

    companion object {
        val Empty = SyncConflictsDialogParams()
    }
}
