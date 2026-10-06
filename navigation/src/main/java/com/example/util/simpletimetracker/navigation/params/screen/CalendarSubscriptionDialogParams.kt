package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CalendarSubscriptionDialogParams(
    val id: String = "",
    val initialName: String = "",
    val initialUrl: String = "",
    val initialColor: String = "",
    val isEnabled: Boolean = true,
) : Parcelable, ScreenParams {

    companion object {
        val Empty = CalendarSubscriptionDialogParams()
    }
}
