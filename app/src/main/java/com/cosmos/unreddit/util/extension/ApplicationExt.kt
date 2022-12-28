package com.cosmos.unreddit.util.extension

import android.content.Context
import android.content.Intent

/**
 * @see <a href="https://stackoverflow.com/a/46848226">StackOverflow answer</a>
 */
fun Context.restart() {
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}
