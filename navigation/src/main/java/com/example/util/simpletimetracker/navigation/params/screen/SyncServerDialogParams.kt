package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SyncServerDialogParams(
    val initialUrl: String = "",
    val initialUsername: String = "",
) : Parcelable, ScreenParams {

    companion object {
        val Empty = SyncServerDialogParams(
            initialUrl = "",
            initialUsername = "",
        )
    }
}
