package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class SyncCredentialsInputType : Parcelable {
    Username,
    Token,
}

@Parcelize
data class SyncCredentialsDialogParams(
    val inputType: SyncCredentialsInputType = SyncCredentialsInputType.Username,
    val initialUsername: String = "",
) : Parcelable, ScreenParams {
    companion object {
        val Empty = SyncCredentialsDialogParams(
            inputType = SyncCredentialsInputType.Username,
            initialUsername = "",
        )
    }
}
