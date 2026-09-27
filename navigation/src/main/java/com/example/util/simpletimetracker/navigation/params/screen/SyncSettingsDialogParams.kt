package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SyncSettingsDialogParams(
    val field: SyncSettingsField = SyncSettingsField.SERVER,
) : Parcelable, ScreenParams
