package com.cosmos.unreddit

import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.cosmos.unreddit.MainActivity.BottomNavigationState.LEFT_HANDED
import com.cosmos.unreddit.MainActivity.BottomNavigationState.NOT_INITIALIZED
import com.cosmos.unreddit.MainActivity.BottomNavigationState.RIGHT_HANDED
import com.cosmos.unreddit.databinding.ActivityMainBinding
import com.cosmos.unreddit.ui.policydisclaimer.PolicyDisclaimerDialogFragment
import com.cosmos.unreddit.ui.postlist.PostListFragment
import com.cosmos.unreddit.util.HideBottomViewBehavior
import com.cosmos.unreddit.util.extension.clearWindowInsetsListener
import com.cosmos.unreddit.util.extension.currentNavigationFragment
import com.cosmos.unreddit.util.extension.isPast
import com.cosmos.unreddit.util.extension.launchRepeat
import com.cosmos.unreddit.util.extension.unredditApplication
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: UiViewModel by viewModels()

    private lateinit var navController: NavController

    private var bottomNavigationState: BottomNavigationState = NOT_INITIALIZED

    private var policyDisclaimerSnackbar: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(unredditApplication.appTheme)
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initNavigation()

        launchRepeat(Lifecycle.State.STARTED) {
            launch {
                viewModel.navigationVisibility
                    // Drop the first item to let initBottomNavigationView manage the visibility
                    .drop(1)
                    .collect(this@MainActivity::showNavigation)
            }

            launch {
                viewModel.leftHandedMode.collect { leftHandedMode ->
                    when (bottomNavigationState) {
                        NOT_INITIALIZED -> initBottomNavigationView(leftHandedMode)
                        RIGHT_HANDED -> if (leftHandedMode) initBottomNavigationView(true)
                        LEFT_HANDED -> if (!leftHandedMode) initBottomNavigationView(false)
                    }
                }
            }

            launch {
                viewModel.policyDisclaimerShown.collect { shown ->
                    if (!shown && POLICY_DISCLAIMER_DATE.isPast) {
                        // Don't show the snackbar right away
                        delay(POLICY_DISCLAIMER_DELAY)
                        showPolicyDisclaimerSnackbar()
                    }
                }
            }
        }
    }

    private fun initNavigation() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                as NavHostFragment
        navController = navHostFragment.navController.apply {
            addOnDestinationChangedListener(this@MainActivity)
        }

        binding.bottomNavigation.run {
            setupWithNavController(navController)
            setOnItemReselectedListener {
                when (it.itemId) {
                    R.id.home -> (currentNavigationFragment as? PostListFragment)?.scrollToTop()
                    else -> {
                        // Ignore
                    }
                }
            }
        }
    }

    private fun initBottomNavigationView(leftHandedMode: Boolean) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.run {
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    bottomMargin = insets.bottom +
                            resources.getDimension(R.dimen.bottom_navigation_margin).toInt()
                }

                clearWindowInsetsListener()
            }

            windowInsets
        }

        binding.bottomNavigation.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            gravity = if (leftHandedMode) {
                Gravity.BOTTOM or Gravity.START
            } else {
                Gravity.BOTTOM or Gravity.END
            }
            behavior = HideBottomViewBehavior<BottomNavigationView>(leftHandedMode)
        }

        val radius = resources.getDimension(R.dimen.bottom_navigation_radius)
        val bottomNavigationBackground = binding.bottomNavigation.background
                as? MaterialShapeDrawable

        bottomNavigationBackground?.run {
            val builder = shapeAppearanceModel.toBuilder()

            if (leftHandedMode) {
                builder.apply {
                    setTopRightCorner(CornerFamily.ROUNDED, radius)
                    setBottomRightCorner(CornerFamily.ROUNDED, radius)

                    setTopLeftCorner(CornerFamily.CUT, 0F)
                    setBottomLeftCorner(CornerFamily.CUT, 0F)
                }
            } else {
                builder.apply {
                    setTopRightCorner(CornerFamily.CUT, 0F)
                    setBottomRightCorner(CornerFamily.CUT, 0F)

                    setTopLeftCorner(CornerFamily.ROUNDED, radius)
                    setBottomLeftCorner(CornerFamily.ROUNDED, radius)
                }
            }

            shapeAppearanceModel = builder.build()
        }

        // Wait for the view to be ready to show/hide it (otherwise width could be 0)
        binding.bottomNavigation.post {
            showNavigation(viewModel.navigationVisibility.value, false)
        }

        bottomNavigationState = if (leftHandedMode) LEFT_HANDED else RIGHT_HANDED
    }

    private fun showNavigation(show: Boolean, animate: Boolean = true) {
        val layoutParams = binding.bottomNavigation.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = layoutParams.behavior as HideBottomViewBehavior?

        if (show) {
            behavior?.run {
                enabled = true
                slideIn(binding.bottomNavigation, animate)
            }
        } else {
            behavior?.run {
                enabled = false
                slideOut(binding.bottomNavigation, animate)
            }
        }
    }

    private fun showPolicyDisclaimerSnackbar() {
        policyDisclaimerSnackbar = Snackbar
            .make(
                binding.root,
                getString(
                    R.string.snackbar_policy_disclaimer_message,
                    getString(R.string.app_name)
                ),
                Snackbar.LENGTH_INDEFINITE
            )
            .setAction(R.string.snackbar_policy_disclaimer_action) {
                PolicyDisclaimerDialogFragment.show(supportFragmentManager)
                policyDisclaimerSnackbar = null
            }
            .setActionTextColor(ContextCompat.getColor(this, R.color.white))
            .apply { show() }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        when (destination.id) {
            R.id.postListFragment,
            R.id.subscriptionsFragment,
            R.id.profileFragment,
            R.id.preferencesFragment -> {
                viewModel.setNavigationVisibility(true)
            }

            else -> viewModel.setNavigationVisibility(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        policyDisclaimerSnackbar = null
        bottomNavigationState = NOT_INITIALIZED
    }

    private enum class BottomNavigationState {
        NOT_INITIALIZED, RIGHT_HANDED, LEFT_HANDED
    }

    companion object {
        private val POLICY_DISCLAIMER_DATE = Calendar
            .getInstance()
            .apply { set(1900 + 123, 5, 10) }
            .timeInMillis

        private const val POLICY_DISCLAIMER_DELAY: Long = 5000
    }
}
