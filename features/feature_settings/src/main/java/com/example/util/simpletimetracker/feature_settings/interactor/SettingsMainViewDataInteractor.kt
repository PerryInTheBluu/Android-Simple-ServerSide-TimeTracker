package com.example.util.simpletimetracker.feature_settings.interactor

import com.example.util.simpletimetracker.core.interactor.LanguageInteractor
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_base_adapter.ViewHolderType
import com.example.util.simpletimetracker.core.extension.shiftTimeStamp
import com.example.util.simpletimetracker.core.mapper.TimeMapper
import com.example.util.simpletimetracker.feature_settings.R
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.viewData.FirstDayOfWeekViewData
import com.example.util.simpletimetracker.feature_settings.viewData.SettingsStartOfDayViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsSpinnerNotCheckableViewData
import com.example.util.simpletimetracker.feature_settings.mapper.SettingsMapper
import com.example.util.simpletimetracker.feature_settings.viewData.DarkModeViewData
import com.example.util.simpletimetracker.feature_settings.viewData.LanguageViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsBottomViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsHintViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsSelectorViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsCheckboxViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsSpinnerViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsTextViewData
import com.example.util.simpletimetracker.feature_settings.views.SettingsTopViewData
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

class SettingsMainViewDataInteractor @Inject constructor(
    private val resourceRepo: ResourceRepo,
    private val settingsMapper: SettingsMapper,
    private val prefsInteractor: PrefsInteractor,
    private val languageInteractor: LanguageInteractor,
    private val timeMapper: TimeMapper,
) {

    suspend fun execute(): List<ViewHolderType> {
        val result = mutableListOf<ViewHolderType>()

        result += SettingsTopViewData(
            block = SettingsBlock.MainTop,
        )

        result += SettingsCheckboxViewData(
            block = SettingsBlock.AllowMultitasking,
            title = resourceRepo.getString(R.string.settings_allow_multitasking),
            subtitle = resourceRepo.getString(R.string.settings_allow_multitasking_hint),
            isChecked = prefsInteractor.getAllowMultitasking(),
        )

        val darkModeViewData = loadDarkModeViewData()
        result += SettingsSpinnerViewData(
            block = SettingsBlock.DarkMode,
            title = resourceRepo.getString(R.string.settings_dark_mode),
            value = darkModeViewData.items
                .getOrNull(darkModeViewData.selectedPosition)?.text.orEmpty(),
            items = darkModeViewData.items,
            selectedPosition = darkModeViewData.selectedPosition,
            processSameItemSelected = false,
        )

        val languageViewData = loadLanguageViewData()
        result += SettingsSpinnerViewData(
            block = SettingsBlock.Language,
            title = resourceRepo.getString(R.string.settings_language),
            value = languageViewData.currentLanguageName,
            items = languageViewData.items,
            selectedPosition = -1,
            processSameItemSelected = true,
        ).let(::SettingsSpinnerNotCheckableViewData)

        result += SettingsTextViewData(
            block = SettingsBlock.Categories,
            title = resourceRepo.getString(R.string.settings_edit_categories),
            subtitle = resourceRepo.getString(R.string.settings_edit_categories_hint),
        )

        result += SettingsTextViewData(
            block = SettingsBlock.Archive,
            title = resourceRepo.getString(R.string.settings_archive),
            subtitle = "",
            dividerIsVisible = false,
        )

        val firstDayOfWeekViewData = loadFirstDayOfWeekViewData()
        result += SettingsSpinnerViewData(
            block = SettingsBlock.AdditionalFirstDayOfWeek,
            title = resourceRepo.getString(R.string.settings_first_day_of_week),
            value = firstDayOfWeekViewData.items
                .getOrNull(firstDayOfWeekViewData.selectedPosition)?.text.orEmpty(),
            items = firstDayOfWeekViewData.items,
            selectedPosition = firstDayOfWeekViewData.selectedPosition,
            processSameItemSelected = false,
        )

        val startOfDayViewData = loadStartOfDayViewData()
        result += SettingsSelectorViewData(
            block = SettingsBlock.AdditionalShiftStartOfDay,
            title = resourceRepo.getString(R.string.settings_start_of_day),
            subtitle = startOfDayViewData.hint,
            selectedValue = startOfDayViewData.startOfDayValue,
            bottomSpaceIsVisible = false,
            dividerIsVisible = false,
        )
        result += SettingsHintViewData(
            block = SettingsBlock.AdditionalShiftStartOfDayHint,
            text = resourceRepo.getString(R.string.settings_start_of_day_hint),
            topSpaceIsVisible = false,
        )
        val endOfDayValue = loadEndOfDayValue()
        result += SettingsSelectorViewData(
            block = SettingsBlock.AdditionalShiftEndOfDay,
            title = resourceRepo.getString(R.string.settings_end_of_day),
            subtitle = resourceRepo.getString(R.string.settings_end_of_day_hint),
            selectedValue = endOfDayValue,
            bottomSpaceIsVisible = false,
            dividerIsVisible = false,
        )
        result += SettingsHintViewData(
            block = SettingsBlock.AdditionalShiftEndOfDayHint,
            text = resourceRepo.getString(R.string.settings_end_of_day_hint_value, endOfDayValue),
            topSpaceIsVisible = false,
        )
        result += SettingsBottomViewData(
            block = SettingsBlock.MainBottom,
        )

        return result
    }

    private suspend fun loadDarkModeViewData(): DarkModeViewData {
        return prefsInteractor.getSelectedDarkMode()
            .let(settingsMapper::toDarkModeViewData)
    }

    private fun loadLanguageViewData(): LanguageViewData {
        return languageInteractor.getCurrentLanguage()
            .let(settingsMapper::toLanguageViewData)
    }
    private suspend fun loadFirstDayOfWeekViewData(): FirstDayOfWeekViewData {
        return prefsInteractor.getFirstDayOfWeek()
            .let(settingsMapper::toFirstDayOfWeekViewData)
    }

    private suspend fun loadEndOfDayValue(): String {
        val shift = prefsInteractor.getEndOfDayShift()
        return if (shift <= 0L) {
            resourceRepo.getString(R.string.change_record_type_goal_time_disabled)
        } else {
            // The value is the absolute clock time of the day end,
            // displayed as a duration from midnight, e.g. 23:30.
            timeMapper.formatDuration(shift / 1000)
        }
    }

    private suspend fun loadStartOfDayViewData(): SettingsStartOfDayViewData {
        val shift = prefsInteractor.getStartOfDayShift()
        val useMilitaryTime = prefsInteractor.getUseMilitaryTimeFormat()
        val calendar = Calendar.getInstance()

        val hint = resourceRepo.getString(
            R.string.settings_start_of_day_hint_value,
            timeMapper.formatDateTime(
                time = calendar.shiftTimeStamp(timeMapper.getStartOfDayTimeStamp(), shift),
                useMilitaryTime = useMilitaryTime,
                showSeconds = false,
            ),
        )
        val value = if (shift == 0L) {
            resourceRepo.getString(R.string.change_record_type_goal_time_disabled)
        } else {
            timeMapper.formatDuration(abs(shift) / 1000)
        }

        return SettingsStartOfDayViewData(
            startOfDayValue = value,
            startOfDaySign = settingsMapper.toStartOfDaySign(shift),
            hint = hint,
        )
    }
}
