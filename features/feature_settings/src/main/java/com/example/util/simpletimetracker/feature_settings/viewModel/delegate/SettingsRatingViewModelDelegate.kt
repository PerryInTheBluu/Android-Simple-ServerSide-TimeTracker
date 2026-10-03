package com.example.util.simpletimetracker.feature_settings.viewModel.delegate

import com.example.util.simpletimetracker.core.base.ViewModelDelegate
import com.example.util.simpletimetracker.feature_settings.api.SettingsBlock
import com.example.util.simpletimetracker.feature_settings.interactor.SettingsRatingViewDataInteractor
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.params.screen.DebugMenuDialogParams
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsRatingViewModelDelegate @Inject constructor(
    private val router: Router,
    private val settingsRatingViewDataInteractor: SettingsRatingViewDataInteractor,
) : SettingsDelegate, ViewModelDelegate() {

    private var parent: SettingsParent? = null
    private var debugUnlocked = false
    private var debugClicksCount: Int = 0

    override fun init(parent: SettingsParent) {
        this.parent = parent
    }

    override fun onHidden() {
        debugClicksCount = 0
    }

    override suspend fun getViewData(): SettingsDelegate.ViewData {
        return SettingsDelegate.ViewData(
            key = Companion,
            data = settingsRatingViewDataInteractor.execute(debugUnlocked),
        )
    }

    override fun onBlockClicked(block: SettingsBlock) {
        when (block) {
            SettingsBlock.Version -> onVersionClick()
            SettingsBlock.DebugMenu -> onDebugMenuClick()
            else -> {
                // Do nothing
            }
        }
    }

    private fun onVersionClick() {
        debugClicksCount += 1
        if (debugClicksCount >= DEBUG_CLICKS_TO_UNLOCK) {
            debugUnlocked = true
            delegateScope.launch { parent?.updateContent() }
        }
    }

    private fun onDebugMenuClick() {
        router.navigate(DebugMenuDialogParams)
    }

    companion object : SettingsDelegate.Key {
        private const val DEBUG_CLICKS_TO_UNLOCK = 5
    }
}
