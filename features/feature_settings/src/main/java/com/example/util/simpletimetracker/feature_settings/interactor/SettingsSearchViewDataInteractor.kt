package com.example.util.simpletimetracker.feature_settings.interactor

import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_base_adapter.commentField.CommentFieldViewData
import com.example.util.simpletimetracker.feature_settings.R
import com.example.util.simpletimetracker.feature_settings.views.SettingsTextViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsTextWithIconViewData
import javax.inject.Inject

/**
 * Search field for the settings list and title based filtering of the
 * settings rows. While a query is active only matching text rows are
 * shown; structural rows (headers, hints, dividers) are hidden.
 */
class SettingsSearchViewDataInteractor @Inject constructor(
    private val resourceRepo: ResourceRepo,
) {

    fun searchField(): CommentFieldViewData {
        return CommentFieldViewData(
            id = SEARCH_FIELD_ID,
            text = null,
            marginTopDp = 0,
            marginHorizontal = 8,
            hint = resourceRepo.getString(R.string.settings_search_hint),
            valueType = CommentFieldViewData.ValueType.TextSingleLine,
        )
    }

    fun filter(
        content: List<ViewHolderType>,
        query: String,
    ): List<ViewHolderType> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return content
        return content.mapNotNull { item ->
            val title = when (item) {
                is SettingsTextViewData -> item.title
                is SettingsTextWithIconViewData -> item.data.title
                else -> null
            }?.lowercase()
            if (title != null && title.contains(normalized)) item else null
        }
    }

    companion object {
        val SEARCH_FIELD_ID = "settings_search".hashCode().toLong()
    }
}
