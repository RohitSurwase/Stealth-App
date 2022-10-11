package com.cosmos.unreddit.util.extension

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.cosmos.unreddit.R
import com.cosmos.unreddit.UnredditApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun AppCompatActivity.launchRepeat(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        repeatOnLifecycle(state) {
            block()
        }
    }
}

val Activity.unredditApplication: UnredditApplication
    get() = application as UnredditApplication

val FragmentActivity.currentNavigationFragment: Fragment?
    get() = supportFragmentManager.findFragmentById(R.id.fragment_container)
        ?.childFragmentManager
        ?.fragments
        ?.firstOrNull()
