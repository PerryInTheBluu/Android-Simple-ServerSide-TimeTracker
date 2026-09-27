package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/** Which sync credential the dialog edits. */
@Parcelize
enum class SyncSettingsField : Parcelable {
    SERVER,
    USERNAME,
    TOKEN,
}
