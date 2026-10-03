package com.example.util.simpletimetracker.feature_main.provider

import androidx.annotation.DrawableRes
import com.example.util.simpletimetracker.core.model.NavigationTab
import com.example.util.simpletimetracker.core.model.NavigationTabProvider
import com.example.util.simpletimetracker.core.repo.ResourceRepo
import com.example.util.simpletimetracker.core.utils.SHORTCUT_NAVIGATION_RECORDS
import com.example.util.simpletimetracker.core.utils.SHORTCUT_NAVIGATION_SETTINGS
import com.example.util.simpletimetracker.core.utils.SHORTCUT_NAVIGATION_STATISTICS
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.prefs.interactor.PrefsInteractor
import com.example.util.simpletimetracker.feature_main.R
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class MainTabsProvider @Inject constructor(
    val tabProviders: Map<Class<out NavigationTab>, @JvmSuppressWildcards NavigationTabProvider>,
    private val resourceRepo: ResourceRepo,
    private val prefsInteractor: PrefsInteractor,
) {

    val tabsList: List<NavigationTab> by lazy { loadTabsList() }

    /** Tabs of the left pane in the two pane tablet layout. */
    val startTabsList: List<NavigationTab> by lazy {
        tabsList.filter { it is NavigationTab.RunningRecords || it is NavigationTab.Uni }
    }

    /** Tabs of the right pane in the two pane tablet layout. */
    val restTabsList: List<NavigationTab> by lazy {
        tabsList.filterNot { it is NavigationTab.RunningRecords || it is NavigationTab.Uni }
    }

    val mainTab: NavigationTab by lazy { loadMainTab() }

    fun mapTabToIcon(tab: NavigationTab?): Int = tab.let(::mapToIcon)

    fun mapTabToDescription(tab: NavigationTab?): String? = tab.let(::mapToDescription)

    fun mapNavigationToTab(value: String): NavigationTab? {
        return mapNavigationToTabInternal(value)
    }

    fun mapPositionToTab(position: Int): NavigationTab? {
        return tabsList.getOrNull(position)
    }

    fun mapTabToPosition(tab: NavigationTab): Int {
        return tabsList.indexOf(tab).takeUnless { it == -1 }.orZero()
    }

    @DrawableRes
    fun mapPositionToIcon(position: Int): Int {
        return position
            .let(::mapPositionToTab)
            .let(::mapToIcon)
    }

    fun mapPositionToDescription(position: Int): String? {
        return position
            .let(::mapPositionToTab)
            .let(::mapToDescription)
    }

    fun mapNavigationToPosition(value: String): Int? {
        return mapNavigationToTabInternal(value)?.let(::mapTabToPosition)
    }

    @DrawableRes
    private fun mapToIcon(tab: NavigationTab?): Int {
        return when (tab) {
            NavigationTab.RunningRecords -> R.drawable.tab_running_records
            NavigationTab.Uni -> R.drawable.tab_uni
            NavigationTab.Records -> R.drawable.tab_records
            NavigationTab.Statistics -> R.drawable.tab_statistics
            NavigationTab.Settings -> R.drawable.tab_settings
            NavigationTab.Goals -> R.drawable.tab_goals
            NavigationTab.Todos -> R.drawable.tab_todos
            null -> R.drawable.unknown
        }
    }

    private fun mapToDescription(tab: NavigationTab?): String? {
        return when (tab) {
            NavigationTab.RunningRecords -> R.string.shortcut_navigation_timers
            NavigationTab.Uni -> R.string.uni_tab_name
            NavigationTab.Records -> R.string.shortcut_navigation_records
            NavigationTab.Statistics -> R.string.shortcut_navigation_statistics
            NavigationTab.Settings -> R.string.shortcut_navigation_settings
            NavigationTab.Goals -> R.string.change_record_type_goal_time_hint
            NavigationTab.Todos -> R.string.todos_tab_name
            null -> return null
        }.let(resourceRepo::getString)
    }

    private fun mapNavigationToTabInternal(value: String): NavigationTab? {
        return when (value) {
            SHORTCUT_NAVIGATION_RECORDS -> NavigationTab.Records
            SHORTCUT_NAVIGATION_STATISTICS -> NavigationTab.Statistics
            SHORTCUT_NAVIGATION_SETTINGS -> NavigationTab.Settings
            else -> null
        }
    }

    private fun loadTabsList(): List<NavigationTab> {
        val showGoals = runBlocking { prefsInteractor.getShowGoalsSeparately() }
        val showUniTab = runBlocking { prefsInteractor.getShowUniTab() }

        return listOfNotNull(
            NavigationTab.RunningRecords,
            NavigationTab.Uni.takeIf { showUniTab },
            NavigationTab.Records,
            NavigationTab.Goals.takeIf { showGoals },
            NavigationTab.Todos,
            NavigationTab.Statistics,
            NavigationTab.Settings,
        )
    }

    private fun loadMainTab(): NavigationTab {
        return NavigationTab.Goals
    }
}