package com.example.util.simpletimetracker.feature_running_records.di

import com.example.util.simpletimetracker.core.model.NavigationTab
import com.example.util.simpletimetracker.core.model.NavigationTabKey
import com.example.util.simpletimetracker.core.model.NavigationTabProvider
import com.example.util.simpletimetracker.feature_running_records.api.OnShortcutClickInteractor
import com.example.util.simpletimetracker.feature_running_records.api.RecordsShortcutsViewDataInteractor
import com.example.util.simpletimetracker.feature_running_records.interactor.OnShortcutClickInteractorImpl
import com.example.util.simpletimetracker.feature_running_records.interactor.RecordsShortcutsViewDataInteractorImpl
import com.example.util.simpletimetracker.feature_running_records.view.RunningRecordsFragment
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
interface RunningRecordsModule {

    @Binds
    fun bindRecordsShortcutsViewDataInteractor(impl: RecordsShortcutsViewDataInteractorImpl): RecordsShortcutsViewDataInteractor

    @Binds
    fun bindOnShortcutClickInteractor(impl: OnShortcutClickInteractorImpl): OnShortcutClickInteractor

    companion object {
        @Provides
        @IntoMap
        @NavigationTabKey(NavigationTab.RunningRecords::class)
        fun bindNavigationTab(): NavigationTabProvider {
            return NavigationTabProvider { RunningRecordsFragment.newInstance() }
        }

        @Provides
        @IntoMap
        @NavigationTabKey(NavigationTab.Uni::class)
        fun bindUniNavigationTab(): NavigationTabProvider {
            return NavigationTabProvider { RunningRecordsFragment.newInstance(uniMode = true) }
        }
    }
}