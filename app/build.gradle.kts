import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    // kapt is still required by data binding; Hilt and Room ride along on it. Moshi's codegen
    // dropped kapt support and runs through KSP.
    kotlin("kapt")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
}

val keystorePropertiesFile = file("../keys/keystore.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} else {
    keystoreProperties["storeFile"] = "keystore.jks"
}

android {
    namespace = Config.namespace

    compileSdk = Config.compileSdk

    defaultConfig {
        applicationId = Config.applicationId

        minSdk = Config.minSdk
        targetSdk = Config.targetSdk

        versionCode = Config.versionCode
        versionName = Config.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables.useSupportLibrary = true

        javaCompileOptions {
            annotationProcessorOptions {
                compilerArgumentProviders(RoomSchemaArgProvider(File(projectDir, "schemas")))
            }
        }
    }

    signingConfigs {
        // Only declared when keys/keystore.properties is present, so that a checkout without the
        // signing material can still build release (unsigned) for verification.
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias", "")
                keyPassword = keystoreProperties.getProperty("keyPassword", "")
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties.getProperty("storePassword", "")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".dev"
            isDebuggable = true
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        // AGP 8 stopped generating BuildConfig by default; APPLICATION_ID and VERSION_NAME are
        // both read at runtime.
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi")
    }
}

dependencies {
    implementation("com.google.dagger:hilt-android:${Dependencies.Versions.hiltGradlePlugin}")
    kapt("com.google.dagger:hilt-android-compiler:${Dependencies.Versions.hiltGradlePlugin}")
    kapt("androidx.hilt:hilt-compiler:${Dependencies.Versions.hilt}")

    implementation("androidx.hilt:hilt-navigation-fragment:${Dependencies.Versions.hilt}")
    implementation("androidx.hilt:hilt-work:${Dependencies.Versions.hilt}")

    implementation("androidx.core:core-ktx:${Dependencies.Versions.core}")
    implementation("androidx.appcompat:appcompat:${Dependencies.Versions.appCompat}")
    implementation("androidx.constraintlayout:constraintlayout:${Dependencies.Versions.constraintLayout}")
    implementation("androidx.recyclerview:recyclerview:${Dependencies.Versions.recyclerview}")
    implementation("androidx.fragment:fragment-ktx:${Dependencies.Versions.fragment}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Dependencies.Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${Dependencies.Versions.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Dependencies.Versions.lifecycle}")
    implementation("androidx.coordinatorlayout:coordinatorlayout:${Dependencies.Versions.coordinatorlayout}")
    implementation("androidx.viewpager2:viewpager2:${Dependencies.Versions.viewpager2}")
    implementation("androidx.preference:preference-ktx:${Dependencies.Versions.preference}")

    implementation("androidx.navigation:navigation-fragment-ktx:${Dependencies.Versions.navigation}")
    implementation("androidx.navigation:navigation-ui-ktx:${Dependencies.Versions.navigation}")

    implementation("androidx.room:room-runtime:${Dependencies.Versions.room}")
    kapt("androidx.room:room-compiler:${Dependencies.Versions.room}")
    implementation("androidx.room:room-ktx:${Dependencies.Versions.room}")
    implementation("androidx.datastore:datastore-preferences:${Dependencies.Versions.datastore}")

    implementation("androidx.paging:paging-runtime-ktx:${Dependencies.Versions.paging}")

    implementation("androidx.work:work-runtime-ktx:${Dependencies.Versions.work}")

    implementation("com.google.android.material:material:${Dependencies.Versions.material}")

    implementation("androidx.browser:browser:${Dependencies.Versions.browser}")

    implementation("androidx.core:core-splashscreen:${Dependencies.Versions.splashscreen}")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Dependencies.Versions.coroutines}")

    implementation("com.squareup.retrofit2:retrofit:${Dependencies.Versions.retrofit}")
    implementation("com.squareup.retrofit2:converter-moshi:${Dependencies.Versions.retrofit}")

    implementation("com.squareup.moshi:moshi:${Dependencies.Versions.moshi}")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:${Dependencies.Versions.moshi}")
    implementation("com.squareup.moshi:moshi-adapters:${Dependencies.Versions.moshi}")

    // Pinned explicitly: the code targets OkHttp 4 / Okio 3 APIs directly, so neither should be
    // left to transitive resolution.
    implementation("com.squareup.okhttp3:okhttp:${Dependencies.Versions.okHttp}")
    implementation("com.squareup.okio:okio:${Dependencies.Versions.okio}")

    implementation("io.coil-kt:coil:${Dependencies.Versions.coil}")
    implementation("io.coil-kt:coil-gif:${Dependencies.Versions.coil}")

    implementation("com.github.MikeOrtiz:TouchImageView:${Dependencies.Versions.touchImageView}")

    implementation("com.google.android.exoplayer:exoplayer:${Dependencies.Versions.exoPlayer}")

    implementation("org.jsoup:jsoup:${Dependencies.Versions.jsoup}")

    implementation("com.drakeet.drawer:drawer:${Dependencies.Versions.drawer}")

    testImplementation("junit:junit:${Dependencies.Versions.jUnit}")

    androidTestImplementation("androidx.test:runner:${Dependencies.Versions.testRunner}")
    androidTestImplementation("androidx.test.ext:junit:${Dependencies.Versions.test}")
    androidTestImplementation("androidx.test.espresso:espresso-core:${Dependencies.Versions.espresso}")
}
