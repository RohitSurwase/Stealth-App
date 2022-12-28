buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Dependencies.Versions.androidGradlePlugin}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Dependencies.Versions.kotlin}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Dependencies.Versions.hiltGradlePlugin}")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Dependencies.Versions.navigation}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
