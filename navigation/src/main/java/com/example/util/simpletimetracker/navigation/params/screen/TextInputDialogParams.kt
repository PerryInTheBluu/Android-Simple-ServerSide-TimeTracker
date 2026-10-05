package com.example.util.simpletimetracker.navigation.params.screen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TextInputDialogParams(
    val tag: String? = null,
    val title: String = "",
    val prefill: String = "",
    val hint: String = "",
) : Parcelable, ScreenParams
