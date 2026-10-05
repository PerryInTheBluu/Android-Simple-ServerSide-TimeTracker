package com.example.util.simpletimetracker.feature_dialogs.viewModel

import androidx.lifecycle.ViewModel
import com.example.util.simpletimetracker.core.base.SingleLiveEvent
import com.example.util.simpletimetracker.core.extension.set
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TextInputViewModel @Inject constructor() : ViewModel() {

    val saveEvent: SingleLiveEvent<Pair<String, String?>> = SingleLiveEvent()

    private var current: String = ""

    fun onTextChange(data: String) {
        current = data
    }

    fun onKeyboardButtonClick() {
        // Nothing to do; the field only needs the keyboard button to
        // dismiss the keyboard on small screens.
    }

    fun onSaveClick() {
        saveEvent.set(current.trim() to null)
    }
}
