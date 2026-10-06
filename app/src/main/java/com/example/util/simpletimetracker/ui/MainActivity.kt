package com.example.util.simpletimetracker.ui

import android.view.LayoutInflater
import androidx.activity.viewModels
import com.example.util.simpletimetracker.core.base.BaseActivity
import com.example.util.simpletimetracker.core.manager.ThemeManager
import com.example.util.simpletimetracker.core.provider.ContextProvider
import com.example.util.simpletimetracker.feature_views.extension.visible
import com.example.util.simpletimetracker.navigation.Router
import com.example.util.simpletimetracker.navigation.RouterProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.util.simpletimetracker.databinding.MainActivityBinding as Binding

@AndroidEntryPoint
class MainActivity : BaseActivity<Binding>(), RouterProvider {

    override val inflater: (LayoutInflater) -> Binding = Binding::inflate

    @Inject
    override lateinit var themeManager: ThemeManager

    @Inject
    override lateinit var contextProvider: ContextProvider

    @Inject
    override lateinit var router: Router

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onResume() {
        super.onResume()
        router.bind(this)
        viewModel.onVisible()
    }

    override fun initUi() {
        router.bind(this)
        router.onCreate(this)
    }

    override fun initViewModel() = with(viewModel) {
        progressVisibility.observe { binding.mainProgress.visible = it }
    }
}
