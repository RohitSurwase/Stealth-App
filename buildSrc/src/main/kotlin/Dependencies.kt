object Dependencies {
    object Versions {
        const val kotlin = "2.1.21"

        // Must track `kotlin`: the version is <kotlin version>-<ksp version>.
        const val ksp = "2.1.21-2.0.2"

        const val androidGradlePlugin = "8.11.1"
        const val hiltGradlePlugin = "2.56.2"

        const val hilt = "1.2.0"

        const val core = "1.16.0"
        const val appCompat = "1.7.1"
        const val constraintLayout = "2.2.1"
        const val recyclerview = "1.4.0"
        const val fragment = "1.8.9"
        const val lifecycle = "2.9.2"
        const val coordinatorlayout = "1.3.0"
        const val viewpager2 = "1.1.0"
        const val preference = "1.2.1"

        const val navigation = "2.9.3"

        const val room = "2.7.2"

        // Ships libdatastore_shared_counter.so; 1.1.2+ aligns it for the 16 KB memory pages that
        // Android 15+ devices use.
        const val datastore = "1.1.7"

        const val paging = "3.3.6"

        const val work = "2.10.2"

        const val material = "1.12.0"

        const val browser = "1.9.0"

        const val splashscreen = "1.0.1"

        const val touchImageView = "3.0.3"

        // 2.19.1 is the final ExoPlayer 2.x release; the library is superseded by androidx.media3.
        const val exoPlayer = "2.19.1"

        const val retrofit = "2.11.0"

        const val moshi = "1.15.2"

        const val okHttp = "4.12.0"

        const val okio = "3.10.2"

        const val coil = "2.7.0"

        const val jsoup = "1.21.1"

        const val drawer = "1.0.3"

        const val coroutines = "1.10.2"

        const val jUnit = "4.13.2"
        const val test = "1.2.1"
        const val testRunner = "1.6.2"
        const val espresso = "3.6.1"
    }
}
