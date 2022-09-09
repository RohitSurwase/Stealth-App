package com.cosmos.unreddit

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.util.CoilUtils
import com.cosmos.unreddit.data.model.preferences.UiPreferences
import com.cosmos.unreddit.data.repository.PreferencesRepository
import com.cosmos.unreddit.util.FileUncaughtExceptionHandler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class UnredditApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    var appTheme: Int = -1
        set(mode) {
            field = if (!UiPreferences.NightMode.isAmoled(mode)) {
                AppCompatDelegate.setDefaultNightMode(mode)
                R.style.AppTheme
            } else {
                // Force dark mode when amoled is set
                AppCompatDelegate.setDefaultNightMode(UiPreferences.NightMode.DARK.mode)
                R.style.AmoledAppTheme
            }
        }

    override fun onCreate() {
        super.onCreate()

        runBlocking {
            val nightMode = preferencesRepository.getNightMode().first()
            appTheme = nightMode
        }

        Thread.setDefaultUncaughtExceptionHandler(FileUncaughtExceptionHandler(this))
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(applicationContext)
            .componentRegistry {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder(applicationContext))
                } else {
                    add(GifDecoder())
                }
            }
            .crossfade(true)
            .okHttpClient {
                OkHttpClient.Builder()
                    .cache(CoilUtils.createDefaultCache(applicationContext))
                    .build()
            }
            .build()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
