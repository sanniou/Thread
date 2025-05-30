import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    //alias(libs.plugins.sqldelight)
}

kotlin {
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs {
//        outputModuleName = "corecommon"
//        browser {
//            val rootDirPath = project.rootDir.path
//            val projectDirPath = project.projectDir.path
//            commonWebpackConfig {
//                outputFileName = "corecommon.js"
//                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
//                    static = (static ?: mutableListOf()).apply {
//                        // Serve sources to debug inside browser
//                        add(rootDirPath)
//                        add(projectDirPath)
//                    }
//                }
//            }
//        }
//        binaries.executable()
//    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "CoreCommon"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
//            implementation(libs.android.driver)
        }
        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            api(libs.navigation.compose)
            api(libs.ktorfit.lib)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.encoding)
            api(libs.ktor.serialization.kotlinx.json)
            api(libs.kodein.di)
            api(libs.kottage)
            api(libs.kotlinx.datetime)
            // pager
            // api(libs.paging.compose.common)
            // api(libs.paging.common)
            // voyager
            api(libs.voyager.navigator)
            api(libs.voyager.screenmodel)
            api(libs.voyager.bottom.sheet.navigator)
            api(libs.voyager.tab.navigator)
            api(libs.voyager.transitions)
            api(libs.voyager.kodein)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.java)
            implementation(libs.kotlinx.coroutinesSwing)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
//            implementation(libs.native.driver)
        }
    }
}

android {
    namespace = "ai.saniou.thread.common"
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
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "ai.saniou.core_common.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ai.saniou.corecommon"
            packageVersion = "1.0.0"
        }
    }
}
