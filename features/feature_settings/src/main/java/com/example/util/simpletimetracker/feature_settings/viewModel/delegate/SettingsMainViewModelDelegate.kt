package com.example.util.simpletimetracker.feature_settings.viewModel.delegate

import com.example.util.simpletimetracker.core.base.SingleLiveEvent
import com.example.util.simpletimetracker.core.base.ViewModelDelegate
import com.example.util.simpletimetracker.core.extension.set
import com.example.util.simpletimetracker.core.interactor.LanguageInteractor
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.R
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_settings.api.OnSettingChangedInteractor
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.interactor.SettingsMainViewDataInteractor
import com.example.util.simpletimetracker.feature_settings.interactor.SettingsOptionsUpdateInteractor
import com.example.util.simpletimetracker.feature_settings.mapper.SettingsMapper
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.ArchiveParams
import com.example.util.simpletimetracker.navigation.params.screen.CategoriesParams
import com.example.util.simpletimetracker.navigation.params.screen.StandardDialogParams
import com.example.util.simpletimetracker.domain.timetable.interactor.CalendarSubscriptionSyncInteractor
import com.example.util.simpletimetracker.domain.timetable.model.CalendarSubscription
import com.example.util.simpletimetracker.domain.notifications.interactor.LocalDataChangedBus
import com.example.util.simpletimetracker.navigation.params.screen.CalendarSubscriptionDialogParams
import com.example.util.simpletimetracker.navigation.params.notification.ToastParams
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsMainViewModelDelegate @Inject constructor(
    private val router: Router,
    private val prefsInteractor: PrefsInteractor,
    private val languageInteractor: LanguageInteractor,
    private val settingsMapper: SettingsMapper,
    private val onSettingChangedInteractor: OnSettingChangedInteractor,
    private val settingsMainViewDataInteractor: SettingsMainViewDataInteractor,
    private val settingsOptionsUpdateInteractor: SettingsOptionsUpdateInteractor,
    private val settingsFileWorkDelegate: SettingsFileWorkDelegate,
    private val calendarSubscriptionSyncInteractor: CalendarSubscriptionSyncInteractor,
    private val resourceRepo: ResourceRepo,
) : SettingsDelegate, ViewModelDelegate() {

    val themeChanged: SingleLiveEvent<Boolean> = SingleLiveEvent()

    private var parent: SettingsParent? = null

    override fun init(parent: SettingsParent) {
        this.parent = parent
    }

    override suspend fun getViewData(): SettingsDelegate.ViewData {
        return SettingsDelegate.ViewData(
            key = Companion,
            data = settingsMainViewDataInteractor.execute(),
        )
    }

    override fun onBlockClicked(block: SettingsBlock) {
        when (block) {
            SettingsBlock.Categories -> onEditCategoriesClick()
            SettingsBlock.Archive -> onArchiveClick()
            SettingsBlock.AllowMultitasking -> onAllowMultitaskingClicked()
            SettingsBlock.TimetableImport -> onTimetableImportClick()
            SettingsBlock.TimetableSubscription -> onTimetableSubscriptionClick()
            SettingsBlock.TimetableSubscriptionSyncNow -> onTimetableSubscriptionSyncNow()
            else -> {
                // Do nothing
            }
        }
    }

    override fun onPositiveClick(tag: String?) {
        if (tag == TIMETABLE_IMPORT_ALERT_DIALOG_TAG) {
            delegateScope.launch {
                settingsOptionsUpdateInteractor.sendDismiss()
                delay(200)
                settingsFileWorkDelegate.onTimetableImportConfirmed()
            }
        }
    }

    override fun onSpinnerPositionSelected(block: SettingsBlock, position: Int) {
        when (block) {
            SettingsBlock.DarkMode -> onDarkModeSelected(position)
            SettingsBlock.Language -> onLanguageSelected(position)
            else -> {
                // Do nothing
            }
        }
    }

    private fun onEditCategoriesClick() {
        router.navigate(CategoriesParams)
    }

    private fun onArchiveClick() {
        router.navigate(ArchiveParams)
    }

    private fun onTimetableImportClick() {
        router.navigate(
            StandardDialogParams(
                tag = TIMETABLE_IMPORT_ALERT_DIALOG_TAG,
                message = resourceRepo.getString(R.string.settings_timetable_import_alert),
                btnPositive = resourceRepo.getString(R.string.ok),
                btnNegative = resourceRepo.getString(R.string.cancel),
            ),
        )
    }

    private fun onAllowMultitaskingClicked() {
        delegateScope.launch {
            val newValue = !prefsInteractor.getAllowMultitasking()
            prefsInteractor.setAllowMultitasking(newValue)
            onSettingChangedInteractor.onAllowMultitaskingChange()
            parent?.updateContent()
        }
    }

    private fun onDarkModeSelected(position: Int) {
        delegateScope.launch {
            val currentMode = prefsInteractor.getSelectedDarkMode()
            val newMode = settingsMapper.toDarkMode(position)
            if (newMode == currentMode) return@launch

            prefsInteractor.setDarkMode(newMode)
            parent?.updateContent()
            themeChanged.set(true)
        }
    }

    private fun onLanguageSelected(position: Int) {
        delegateScope.launch {
            val newLanguage = settingsMapper.toLanguage(position)
            languageInteractor.setLanguage(newLanguage)
            parent?.updateContent()
            router.restartApp()
        }
    }

    private fun onTimetableSubscriptionClick() {
        delegateScope.launch {
            val subscriptions = prefsInteractor.getCalendarSubscriptions()
            val active = subscriptions.firstOrNull { it.enabled } ?: subscriptions.firstOrNull()
            router.navigate(
                CalendarSubscriptionDialogParams(
                    id = active?.id.orEmpty(),
                    initialName = active?.name.orEmpty(),
                    initialUrl = active?.url.orEmpty(),
                    initialColor = active?.color.orEmpty(),
                    isEnabled = active?.enabled ?: true,
                ),
            )
        }
    }

    private fun onTimetableSubscriptionSyncNow() {
        delegateScope.launch {
            val results = calendarSubscriptionSyncInteractor.syncAll()
            val firstSuccess = results.filterIsInstance<CalendarSubscriptionSyncInteractor.SyncResult.Success>().firstOrNull()
            val firstError = results.filterIsInstance<CalendarSubscriptionSyncInteractor.SyncResult.Error>().firstOrNull()

            if (firstSuccess != null) {
                router.show(
                    ToastParams(
                        message = resourceRepo.getString(
                            R.string.settings_timetable_subscription_sync_success,
                            firstSuccess.importResult.eventsAdded,
                            firstSuccess.importResult.overridesAdded,
                        ),
                    ),
                )
                LocalDataChangedBus.publish()
            } else if (firstError != null) {
                router.show(
                    ToastParams(
                        message = resourceRepo.getString(
                            R.string.settings_timetable_subscription_sync_error,
                            firstError.error,
                        ),
                    ),
                )
            }
            parent?.updateContent()
        }
    }

    fun onCalendarSubscriptionSaved(id: String, name: String, url: String, color: String, enabled: Boolean) {
        delegateScope.launch {
            val sub = CalendarSubscription(
                id = id,
                name = name,
                url = url,
                color = color,
                enabled = enabled,
            )
            prefsInteractor.addCalendarSubscription(sub)
            if (enabled) {
                val result = calendarSubscriptionSyncInteractor.syncSubscription(sub, clearExisting = false)
                if (result is CalendarSubscriptionSyncInteractor.SyncResult.Success) {
                    router.show(
                        ToastParams(
                            message = resourceRepo.getString(
                                R.string.settings_timetable_subscription_sync_success,
                                result.importResult.eventsAdded,
                                result.importResult.overridesAdded,
                            ),
                        ),
                    )
                }
                LocalDataChangedBus.publish()
            }
            parent?.updateContent()
        }
    }

    fun onCalendarSubscriptionDeleted(id: String) {
        delegateScope.launch {
            prefsInteractor.removeCalendarSubscription(id)
            LocalDataChangedBus.publish()
            parent?.updateContent()
        }
    }

    companion object : SettingsDelegate.Key {
        private const val TIMETABLE_IMPORT_ALERT_DIALOG_TAG = "TIMETABLE_IMPORT_ALERT_DIALOG_TAG"
    }
}