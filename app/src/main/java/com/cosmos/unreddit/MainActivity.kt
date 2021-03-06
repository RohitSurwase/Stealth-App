package com.cosmos.unreddit

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.cosmos.unreddit.databinding.ActivityMainBinding
import com.cosmos.unreddit.util.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: UiViewModel by viewModels()

    private var currentNavController: LiveData<NavController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Workaround to prevent activity from being created twice
        val newIntent = intent.clone() as Intent
        intent.data = null

        if (savedInstanceState == null) {
            initNavigation()
        }

        currentNavController?.value?.handleDeepLink(newIntent)

        viewModel.navigationVisibility.observe(this, this::showNavigation)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        initNavigation()
    }

    private fun initNavigation() {
        val navGraphIds = listOf(
            R.navigation.home,
            R.navigation.subscriptions,
            R.navigation.settings
        )

        val controller = binding.bottomNavigation.setupWithNavController(
            navGraphIds,
            supportFragmentManager,
            R.id.fragment_container,
            intent
        )

        currentNavController = controller
    }

    private fun showNavigation(show: Boolean) {
        val transition = Slide().apply {
            duration = 500
            addTarget(binding.bottomNavigation)
        }
        TransitionManager.beginDelayedTransition(binding.root, transition)
        binding.bottomNavigation.visibility = if (show) View.VISIBLE else View.GONE
    }
}
