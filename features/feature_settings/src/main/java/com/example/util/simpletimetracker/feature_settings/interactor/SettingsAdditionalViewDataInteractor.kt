package com.example.util.simpletimetracker.feature_settings.interactor

import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.feature_settings.R
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.mapper.SettingsMapper
import com.example.util.simpletimetracker.feature_settings.viewData.RepeatButtonViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsBottomViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsCheckboxViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsCheckboxWithButtonViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsCollapseViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsSelectorViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsSpinnerEvenViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsSpinnerViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsTextViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsTextWithButtonViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsTopViewData
import javax.inject.Inject

class SettingsAdditionalViewDataInteractor @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val settingsMapper: SettingsMapper,
    private val prefsInteractor: PrefsInteractor,
    private val timeMapper: TimeMapper,
) {

    suspend fun execute(
        isCollapsed: Boolean,
    ): List<ViewHolderType> {
        val isDarkTheme = prefsInteractor.getDarkMode()
        val result = mutableListOf<ViewHolderType>()

        result += SettingsTopViewData(
            block = SettingsBlock.AdditionalTop,
        )

        result += SettingsCollapseViewData(
            block = SettingsBlock.AdditionalCollapse,
            title = resourceRepo.getString(R.string.settings_additional_title),
            opened = !isCollapsed,
            iconResId = R.drawable.plus,
            iconColor = (if (isDarkTheme) R.color.blue_300 else R.color.blue_200)
                .let(resourceRepo::getColor),
            dividerIsVisible = !isCollapsed,
        )

        if (!isCollapsed) {
            result += SettingsSelectorViewData(
                block = SettingsBlock.AdditionalIgnoreShort,
                title = resourceRepo.getString(R.string.settings_ignore_short_records),
                subtitle = resourceRepo.getString(R.string.settings_ignore_short_records_hint),
                selectedValue = loadIgnoreShortRecordsViewData(),
                bottomSpaceIsVisible = true,
                dividerIsVisible = true,
            )

            val showRecordTagSelection = prefsInteractor.getShowRecordTagSelection()
            result += SettingsCheckboxWithButtonViewData(
                data = SettingsCheckboxViewData(
                    block = SettingsBlock.AdditionalShowTagSelection,
                    title = resourceRepo.getString(R.string.settings_show_record_tag_selection),
                    subtitle = resourceRepo.getString(R.string.settings_show_record_tag_selection_hint),
                    isChecked = showRecordTagSelection,
                    bottomSpaceIsVisible = !showRecordTagSelection,
                    dividerIsVisible = !showRecordTagSelection,
                ),
                buttonBlock = SettingsBlock.AdditionalTagSelectionExcludeActivities,
                isButtonVisible = showRecordTagSelection,
            )
            if (showRecordTagSelection) {
                val closeAfterOne = prefsInteractor.getRecordTagSelectionCloseAfterOne()
                result += SettingsCheckboxWithButtonViewData(
                    data = SettingsCheckboxViewData(
                        block = SettingsBlock.AdditionalCloseAfterOneTag,
                        title = resourceRepo.getString(R.string.settings_show_record_tag_close_hint),
                        subtitle = "",
                        isChecked = closeAfterOne,
                        bottomSpaceIsVisible = true,
                        dividerIsVisible = true,
                    ),
                    buttonBlock = SettingsBlock.AdditionalCloseAfterOneTagExcludeActivities,
                    isButtonVisible = closeAfterOne,
                )
            }

            val showCommentInput = prefsInteractor.getShowCommentInput()
            result += SettingsCheckboxWithButtonViewData(
                data = SettingsCheckboxViewData(
                    block = SettingsBlock.AdditionalShowCommentInput,
                    title = resourceRepo.getString(R.string.settings_show_comment_input),
                    subtitle = resourceRepo.getString(R.string.settings_show_comment_input_hint),
                    isChecked = showCommentInput,
                    bottomSpaceIsVisible = true,
                    dividerIsVisible = true,
                ),
                buttonBlock = SettingsBlock.AdditionalCommentInputExcludeActivities,
                isButtonVisible = showCommentInput,
            )

            result += SettingsCheckboxViewData(
                block = SettingsBlock.AdditionalKeepStatisticsRange,
                title = resourceRepo.getString(R.string.settings_keep_statistics_range),
                subtitle = resourceRepo.getString(R.string.settings_keep_statistics_range_hint),
                isChecked = prefsInteractor.getKeepStatisticsRange(),
                bottomSpaceIsVisible = true,
                dividerIsVisible = true,
            )

            result += SettingsCheckboxViewData(
                block = SettingsBlock.AdditionalRetroactiveTrackingMode,
                title = resourceRepo.getString(R.string.settings_retroactive_tracking_mode),
                subtitle = resourceRepo.getString(R.string.settings_retroactive_tracking_mode_hint),
                isChecked = prefsInteractor.getRetroactiveTrackingMode(),
                bottomSpaceIsVisible = true,
                dividerIsVisible = true,
            )

            result += SettingsCheckboxViewData(
                block = SettingsBlock.AdditionalKeepScreenOn,
                title = resourceRepo.getString(R.string.settings_keep_screen_on),
                subtitle = "",
                isChecked = prefsInteractor.getKeepScreenOn(),
                bottomSpaceIsVisible = true,
                dividerIsVisible = true,
            )

            result += SettingsCheckboxViewData(
                block = SettingsBlock.AdditionalStartTimerByLongClick,
                title = resourceRepo.getString(R.string.settings_start_timer_by_long_click),
                subtitle = "",
                isChecked = prefsInteractor.getStartTimerByLongClick(),
                bottomSpaceIsVisible = true,
                dividerIsVisible = true,
            )

            val enableRepeatButton = prefsInteractor.getEnableRepeatButton()
            result += SettingsCheckboxViewData(
                block = SettingsBlock.DisplayEnableRepeatButton,
                title = resourceRepo.getString(R.string.settings_show_repeat_button),
                subtitle = "",
                isChecked = enableRepeatButton,
                bottomSpaceIsVisible = !enableRepeatButton,
                dividerIsVisible = !enableRepeatButton,
            )
            if (enableRepeatButton) {
                val repeatButtonViewData = loadRepeatButtonViewData()
                result += SettingsSpinnerViewData(
                    block = SettingsBlock.DisplayRepeatButtonMode,
                    title = resourceRepo.getString(R.string.settings_repeat_button_type),
                    value = repeatButtonViewData.items
                        .getOrNull(repeatButtonViewData.selectedPosition)?.text.orEmpty(),
                    items = repeatButtonViewData.items,
                    selectedPosition = repeatButtonViewData.selectedPosition,
                    processSameItemSelected = false,
                ).let(::SettingsSpinnerEvenViewData)
            }
            val enablePomodoroMode = prefsInteractor.getEnablePomodoroMode()
            result += SettingsCheckboxWithButtonViewData(
                data = SettingsCheckboxViewData(
                    block = SettingsBlock.DisplayEnablePomodoroMode,
                    title = resourceRepo.getString(R.string.settings_enable_pomodoro_mode),
                    subtitle = "",
                    isChecked = enablePomodoroMode,
                    bottomSpaceIsVisible = true,
                    dividerIsVisible = true,
                ),
                buttonBlock = SettingsBlock.DisplayPomodoroModeActivities,
                isButtonVisible = enablePomodoroMode,
            )
            result += SettingsTextWithButtonViewData(
                buttonBlock = SettingsBlock.AdditionalAutomatedTracking,
                data = SettingsTextViewData(
                    block = SettingsBlock.AdditionalAutomatedTracking,
                    title = resourceRepo.getString(R.string.settings_automated_tracking),
                    subtitle = "",
                    dividerIsVisible = false,
                    layoutIsClickable = false,
                ),
            )
            result += SettingsCheckboxViewData(
                block = SettingsBlock.AdditionalSendEvents,
                title = resourceRepo.getString(R.string.settings_automated_tracking_send_events),
                subtitle = "",
                isChecked = prefsInteractor.getAutomatedTrackingSendEvents(),
                topSpaceIsVisible = false,
                dividerIsVisible = false,
            )
            result += SettingsCheckboxViewData(
                block = SettingsBlock.AdditionalReceiveQueries,
                title = resourceRepo.getString(R.string.settings_automated_tracking_receive_queries),
                subtitle = "",
                isChecked = prefsInteractor.getAutomatedTrackingReceiveQueries(),
                topSpaceIsVisible = false,
            )
            result += SettingsTextViewData(
                block = SettingsBlock.AdditionalDataEdit,
                title = resourceRepo.getString(R.string.settings_data_edit),
                subtitle = "",
            )
            result += SettingsTextViewData(
                block = SettingsBlock.AdditionalComplexRules,
                title = resourceRepo.getString(R.string.settings_complex_rules),
                subtitle = "",
            )
            result += SettingsTextViewData(
                block = SettingsBlock.AdditionalActivitySuggestions,
                title = resourceRepo.getString(R.string.settings_activity_suggestions),
                subtitle = "",
            )
            result += SettingsTextViewData(
                block = SettingsBlock.AdditionalShortcuts,
                title = resourceRepo.getString(R.string.change_record_shortcut),
                subtitle = "",
                dividerIsVisible = false,
            )
        }

        result += SettingsBottomViewData(
            block = SettingsBlock.AdditionalBottom,
        )

        return result
    }

    private suspend fun loadRepeatButtonViewData(): RepeatButtonViewData {
        return prefsInteractor.getRepeatButtonType()
            .let(settingsMapper::toRepeatButtonViewData)
    }

    private suspend fun loadIgnoreShortRecordsViewData(): String {
        return prefsInteractor.getIgnoreShortRecordsDuration()
            .let(settingsMapper::toDurationViewData)
            .text
    }
}