import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.sqldelight)
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
            implementation(libs.sqldelight.android.driver)
        }
        commonMain.dependencies {
            implementation(project(":core-ui"))
            implementation(project(":core-common"))
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.voyager.navigator)
            implementation(libs.material3)
            implementation(libs.material3.window.size)
            implementation(libs.material3.adaptive.navigation)
            implementation("org.jetbrains.compose.ui:ui-backhandler:1.8.0")
            implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.1.0")
            implementation("org.jetbrains.compose.material3.adaptive:adaptive-layout:1.1.0")
            implementation("org.jetbrains.compose.material3.adaptive:adaptive-navigation:1.1.0")

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

            implementation(libs.cash.paging)
            implementation(libs.cash.paging.common)
            // https://saket.github.io/telephoto/zoomable-peek-overlay/
            // not multi platform
            // implementation("me.saket.telephoto:zoomable-image-coil3:0.15.1")
            // for multi platform but not image subsampling
            // implementation("me.saket.telephoto:zoomable-peek-overlay:0.15.1")
            // implementation("me.saket.telephoto:zoomable-peek-overlay:0.15.1")
//            implementation(libs.room.runtime)
//            implementation(libs.sqlite.bundled)
            implementation(libs.runtime)
            implementation(libs.sqldelight.paging3)
            implementation(libs.sqldelight.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqldelight.sqlite.driver)
        }

        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
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

sqldelight {
    databases {
        create("Database") {
            packageName.set("ai.saniou.nmb.db")
        }
    }
}

composeCompiler {
    // for sketch
    stabilityConfigurationFile =
        rootProject.layout.projectDirectory.file("compose_compiler_config.conf")
}
