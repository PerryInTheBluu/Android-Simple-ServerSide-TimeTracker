package com.example.util.simpletimetracker.feature_main.view

import android.graphics.ColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.AttrRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.util.simpletimetracker.core.base.BaseFragment
import com.example.util.simpletimetracker.core.di.BaseViewModelFactory
import com.example.util.simpletimetracker.core.model.NavigationTab
import com.example.util.simpletimetracker.core.extension.addOnBackPressedListener
import com.example.util.simpletimetracker.core.extension.addOnPageChangeCallback
import com.example.util.simpletimetracker.core.extension.changeDragSensitivity
import com.example.util.simpletimetracker.core.extension.findRecycler
import com.example.util.simpletimetracker.core.sharedViewModel.MainTabsViewModel
import com.example.util.simpletimetracker.core.utils.InsetConfiguration
import com.example.util.simpletimetracker.core.utils.SHORTCUT_NAVIGATION_KEY
import com.example.util.simpletimetracker.core.view.SafeFragmentStateAdapter
import com.example.util.simpletimetracker.domain.extension.orZero
import com.example.util.simpletimetracker.domain.widget.interactor.WidgetInteractor
import com.example.util.simpletimetracker.feature_main.R
import com.example.util.simpletimetracker.feature_main.adapter.MainContentAdapter
import com.example.util.simpletimetracker.feature_main.provider.MainTabsProvider
import com.example.util.simpletimetracker.feature_main.viewModel.MainViewModel
import com.example.util.simpletimetracker.feature_views.extension.dpToPx
import com.example.util.simpletimetracker.feature_views.extension.getThemedAttr
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.util.simpletimetracker.feature_main.databinding.MainFragmentBinding as Binding

@AndroidEntryPoint
class MainFragment : BaseFragment<Binding>() {

    override val inflater: (LayoutInflater, ViewGroup?, Boolean) -> Binding =
        Binding::inflate

    override var insetConfiguration: InsetConfiguration =
        InsetConfiguration.DoNotApply

    @Inject
    lateinit var mainTabsViewModelFactory: BaseViewModelFactory<MainTabsViewModel>

    @Inject
    lateinit var mainTabsProvider: MainTabsProvider

    @Inject
    lateinit var widgetInteractor: WidgetInteractor

    private val viewModel: MainViewModel by viewModels()
    private val mainTabsViewModel: MainTabsViewModel by activityViewModels(
        factoryProducer = { mainTabsViewModelFactory },
    )

    private val selectedColorFilter by lazy { getColorFilter(R.attr.appTabSelectedColor) }
    private val unselectedColorFilter by lazy { getColorFilter(R.attr.appTabUnselectedColor) }
    private var backPressedCallback: OnBackPressedCallback? = null
    private var shortcutNavigationHandled = false
    private val mainPagePosition by lazy {
        mainTabsProvider.mainTab.let(mainTabsProvider::mapTabToPosition)
    }

    /** On tablets the screen is split: timers on the left, the rest on the right. */
    private val isTwoPaneLayout: Boolean by lazy {
        resources.configuration.smallestScreenWidthDp >= TWO_PANE_MIN_WIDTH_DP
    }

    private val paneTabs: List<NavigationTab> by lazy {
        if (isTwoPaneLayout) mainTabsProvider.restTabsList else mainTabsProvider.tabsList
    }

    override fun initUi() {
        setupPager()
        checkForShortcutNavigation()
        widgetInteractor.initializeCachedViews()
    }

    override fun initUx() {
        backPressedCallback = addOnBackPressedListener(false, ::onBackPressed)
    }

    override fun initViewModel() {
        viewModel.initialize
        mainTabsViewModel.isNavBatAtTheBottom.observe(::updateNavBarPosition)
    }

    private fun setupPager() = with(binding) {
        if (isTwoPaneLayout) {
            setupTwoPanePagers()
            return@with
        }
        setupPagers(startPane = false)
    }

    private fun setupTwoPanePagers() = with(binding) {
        mainStartTabs.isVisible = true
        mainStartPager.isVisible = true
        mainPaneDivider.isVisible = true

        setupPaneConstraints()
        setupPagers(startPane = false)
        setupPagers(startPane = true)
    }

    private fun setupPaneConstraints() = with(binding) {
        val set = ConstraintSet()
        set.clone(containerMain)
        set.clear(R.id.mainTabs, ConstraintSet.START)
        set.connect(R.id.mainTabs, ConstraintSet.START, R.id.mainPaneDividerGuideline, ConstraintSet.END)
        set.connect(R.id.mainTabs, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        set.clear(R.id.mainPager, ConstraintSet.START)
        set.connect(R.id.mainPager, ConstraintSet.START, R.id.mainPaneDividerGuideline, ConstraintSet.END)
        set.connect(R.id.mainPager, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        set.applyTo(containerMain)
    }

    private fun setupPagers(
        startPane: Boolean,
    ) = with(binding) {
        val pager = if (startPane) mainStartPager else mainPager
        val tabsView = if (startPane) mainStartTabs else mainTabs
        val tabList = if (startPane) mainTabsProvider.startTabsList else paneTabs

        pager.adapter = SafeFragmentStateAdapter(
            MainContentAdapter(
                fragment = this@MainFragment,
                tabs = tabList,
                providers = mainTabsProvider.tabProviders,
            ),
        )
        pager.offscreenPageLimit = tabList.size - 1 // Same as number of pages to avoid recreating.
        pager.addOnPageChangeCallback(lifecycleOwner = this@MainFragment) { state ->
            mainTabsViewModel.onScrollStateChanged(
                isScrolling = state != ViewPager2.SCROLL_STATE_IDLE,
            )
        }

        TabLayoutMediator(tabsView, pager) { tab, position ->
            tabList.getOrNull(position)
                .let(mainTabsProvider::mapTabToIcon)
                .let(tab::setIcon)
            tabList.getOrNull(position)
                ?.let(mainTabsProvider::mapTabToDescription)
                ?.let { tab.contentDescription = it }
            tab.icon?.colorFilter = if (!startPane && position == mainPagePosition) {
                selectedColorFilter
            } else {
                unselectedColorFilter
            }
        }.attach()

        tabsView.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabReselected(tab: TabLayout.Tab?) {
                    tab?.position
                        ?.let(tabList::getOrNull)
                        ?.let(mainTabsViewModel::onTabReselected)
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    tab?.icon?.colorFilter = unselectedColorFilter
                    tab?.position
                        ?.let(tabList::getOrNull)
                        ?.let(mainTabsViewModel::onTabUnselected)
                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.icon?.colorFilter = selectedColorFilter
                    if (!startPane && !isTwoPaneLayout) {
                        backPressedCallback?.isEnabled = tab?.position.orZero() != mainPagePosition
                    }
                }
            },
        )
        pager.setCurrentItem(if (startPane) 0 else mainPagePosition, false)
        pager.findRecycler()?.changeDragSensitivity(2f)
    }

    private fun updateNavBarPosition(isAtTheBottom: Boolean) = with(binding) {
        if (isTwoPaneLayout) {
            // Two pane layout keeps the tab rows at the top.
            mainTabsDivider.isVisible = false
            return@with
        }
        val set = ConstraintSet()
        set.clone(binding.containerMain)
        if (isAtTheBottom) {
            set.clear(R.id.mainTabs, ConstraintSet.TOP)
            set.connect(R.id.mainTabs, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            set.connect(R.id.mainPager, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            set.connect(R.id.mainPager, ConstraintSet.BOTTOM, R.id.mainTabs, ConstraintSet.TOP)
        } else {
            set.clear(R.id.mainTabs, ConstraintSet.BOTTOM)
            set.connect(R.id.mainTabs, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            set.connect(R.id.mainPager, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            set.connect(R.id.mainPager, ConstraintSet.TOP, R.id.mainTabs, ConstraintSet.BOTTOM)
        }
        set.applyTo(binding.containerMain)

        TabLayout.INDICATOR_GRAVITY_BOTTOM
            .let(mainTabs::setSelectedTabIndicatorGravity)

        mainTabsDivider.isVisible = isAtTheBottom

        (0..<mainTabs.tabCount).forEach { index ->
            val paddingVertical = if (isAtTheBottom) 16.dpToPx() else 0
            mainTabs.getTabAt(index)?.view
                ?.updatePadding(top = paddingVertical, bottom = paddingVertical)
        }

        updateInsetConfiguration(isAtTheBottom)
    }

    private fun onBackPressed() {
        if (isTwoPaneLayout) return
        binding.mainPager.setCurrentItem(mainPagePosition, true)
    }

    private fun checkForShortcutNavigation() = with(binding) {
        if (shortcutNavigationHandled) return@with

        val navigation = activity?.intent?.extras?.getString(SHORTCUT_NAVIGATION_KEY)
        val position = if (isTwoPaneLayout) {
            navigation
                ?.let(mainTabsProvider::mapNavigationToTab)
                ?.let(paneTabs::indexOf)
                ?.takeUnless { it == -1 }
        } else {
            navigation?.let(mainTabsProvider::mapNavigationToPosition)
        }
        position?.let {
            mainPager.setCurrentItem(it, true)
            shortcutNavigationHandled = true
        }
    }

    private fun getColorFilter(@AttrRes attrRes: Int): ColorFilter? {
        return BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            requireContext().getThemedAttr(attrRes),
            BlendModeCompat.SRC_IN,
        )
    }

    private fun updateInsetConfiguration(isNavBatAtTheBottom: Boolean) {
        insetConfiguration = if (isNavBatAtTheBottom) {
            InsetConfiguration.ApplyToView { binding.root }
        } else {
            InsetConfiguration.DoNotApply
        }
        initInsets()
    }

    companion object {
        private const val TWO_PANE_MIN_WIDTH_DP = 600
    }
}
