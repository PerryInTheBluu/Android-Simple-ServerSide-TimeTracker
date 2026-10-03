package com.example.util.simpletimetracker.feature_todos.di

import com.example.util.simpletimetracker.core.model.NavigationTab
import com.example.util.simpletimetracker.core.model.NavigationTabKey
import com.example.util.simpletimetracker.core.model.NavigationTabProvider
import com.example.util.simpletimetracker.feature_todos.view.TodosFragment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
class TodosModule {

    @Provides
    @IntoMap
    @NavigationTabKey(NavigationTab.Todos::class)
    fun bindNavigationTab(): NavigationTabProvider {
        return NavigationTabProvider { TodosFragment.newInstance() }
    }
}
