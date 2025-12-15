import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    sourceSets.all {
        languageSettings.optIn("kotlin.time.ExperimentalTime")
        languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
    }
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "CoreUi"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
        binaries.executable()
    }

//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs {
//        browser()
//        binaries.executable()
//    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.navigation.compose)
            api(libs.navigation.compose)
            api(libs.material.icons)
            api(libs.material.icons.extended)
            // zoom
            implementation(libs.zoom)
            implementation(libs.zoom.sketch)
            implementation(libs.zoom.resources)

            implementation(libs.sketch.compose)
            // implementation("io.github.panpf.sketch4:sketch-http:4.0.6")
            implementation(libs.sketch.animated.gif)
            implementation(libs.sketch.animated.webp)
            implementation(libs.sketch.compose.resources)
            implementation(libs.sketch.extensions.compose.resources)
            implementation(libs.sketch.extensions.compose)
            implementation(libs.sketch.ktor)
            implementation(libs.sketch.svg)
            // Core library
//            implementation(libs.kmpalette.core)
//
//            // Optional extensions based on your image source
//            implementation(libs.kmpalette.extensions.base64)
//            implementation(libs.kmpalette.extensions.bytearray)
//            implementation(libs.kmpalette.extensions.network)
//            implementation(libs.kmpalette.extensions.file)
            implementation(libs.cash.paging.compose.common)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = "ai.saniou.thread.ui"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.ui.android)
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "ai.saniou.core_ui.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ai.saniou.coreui"
            packageVersion = "1.0.0"
        }
    }
}
