import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
}

kotlin {
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
            baseName = "NMB"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.components.resources)
            implementation(project(":core-ui"))
            implementation(project(":core-common"))
            implementation(libs.voyager.navigator)
            implementation(libs.material3)
            implementation(libs.material3.window.size)
            implementation(libs.material3.adaptive.navigation)
            implementation("org.jetbrains.compose.ui:ui-backhandler:1.8.0")
            implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.1.0")
            implementation("org.jetbrains.compose.material3.adaptive:adaptive-layout:1.1.0")
            implementation("org.jetbrains.compose.material3.adaptive:adaptive-navigation:1.1.0")

            // zoom
            // implementation("io.github.panpf.zoomimage:zoomimage-compose-coil3:1.2.0")
            implementation("io.github.panpf.zoomimage:zoomimage-compose:1.2.0")
            implementation("io.github.panpf.zoomimage:zoomimage-compose-sketch4:1.2.0")
            implementation("io.github.panpf.zoomimage:zoomimage-compose-resources:1.2.0")

            implementation("io.github.panpf.sketch4:sketch-compose:4.0.6")
            implementation("io.github.panpf.sketch4:sketch-http:4.0.6")
            // 使用 Android 或 Skia 内置的解码器解码 gif 动图并播放
            implementation("io.github.panpf.sketch4:sketch-animated-gif:4.0.6")
            // 使用 Android 或 Skia 内置的解码器解码 webp 动图并播放
            implementation("io.github.panpf.sketch4:sketch-animated-webp:4.0.6")
            // 支持通过 uri 或 placeholder、fallback、error 访问 compose resources 资源
            implementation("io.github.panpf.sketch4:sketch-compose-resources:4.0.6")
            implementation("io.github.panpf.sketch4:sketch-extensions-compose-resources:4.0.6")
            // 提供下载进度、图片类型角标、列表滚动中暂停加载、节省蜂窝流量等实用功能
            implementation("io.github.panpf.sketch4:sketch-extensions-compose:4.0.6")
            // 支持使用 3.x 版本的 ktor 访问网络图片
            implementation("io.github.panpf.sketch4:sketch-http-ktor3:4.0.6")

            // 支持 SVG 图片
            implementation("io.github.panpf.sketch4:sketch-svg:4.0.6")

            implementation("app.cash.paging:paging-common:3.3.0-alpha02-0.5.1")
            implementation("app.cash.paging:paging-compose-common:3.3.0-alpha02-0.5.1")
            // https://saket.github.io/telephoto/zoomable-peek-overlay/
            // not multi platform
            // implementation("me.saket.telephoto:zoomable-image-coil3:0.15.1")
            // for multi platform but not image subsampling
            // implementation("me.saket.telephoto:zoomable-peek-overlay:0.15.1")
            // implementation("me.saket.telephoto:zoomable-peek-overlay:0.15.1")


        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }

        iosMain.dependencies {

        }
    }
}

android {
    namespace = "ai.saniou.nmb"
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
        mainClass = "ai.saniou.nmb.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ai.saniou.nmb"
            packageVersion = "1.0.0"
        }
    }
}

tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().configureEach {
    if (name.contains("KotlinDesktop")) {
        dependsOn(tasks.withType<com.google.devtools.ksp.gradle.KspAATask>().filter {
            it.name.contains("CommonMainKotlinMetadata")
        })
    }
}
