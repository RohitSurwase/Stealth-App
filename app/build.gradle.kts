import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
}

val keystorePropertiesFile = file("../keystore.properties")
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
        create("release") {
            keyAlias = keystoreProperties.getProperty("keyAlias", "")
            keyPassword = keystoreProperties.getProperty("keyPassword", "")
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties.getProperty("storePassword", "")
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
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".dev"
            isDebuggable = true
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
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

    implementation("com.squareup.retrofit2:retrofit:${Dependencies.Versions.retrofit}")
    implementation("com.squareup.retrofit2:converter-moshi:${Dependencies.Versions.retrofit}")

    implementation("com.squareup.moshi:moshi:${Dependencies.Versions.moshi}")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${Dependencies.Versions.moshi}")
    implementation("com.squareup.moshi:moshi-adapters:${Dependencies.Versions.moshi}")

    implementation("com.squareup.okio:okio:${Dependencies.Versions.moshi}")

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
