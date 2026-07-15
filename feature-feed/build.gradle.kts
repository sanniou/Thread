import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
    }
    android {
        namespace = "ai.saniou.feed"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Feed"
            isStatic = true
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

//    js {
//        browser()
//        binaries.executable()
//    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core-ui"))
            implementation(project(":core-common"))
            implementation(project(":core-domain"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.material3)
            implementation(libs.compose.ui)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.kodein)
            implementation(libs.kodein.di.compose)
            implementation(libs.paging.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}
