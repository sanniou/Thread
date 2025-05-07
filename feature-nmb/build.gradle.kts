import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.google.devtools.ksp") version "2.1.20-2.0.0"
    id("de.jensklingenberg.ktorfit") version "2.5.1"
    kotlin("plugin.serialization") version "2.1.20"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
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
            implementation(libs.kottage)
            implementation(libs.kotlinx.datetime)
            // Coil for Multiplatform
            implementation(libs.coil.compose)
            implementation(libs.coil.network)
//             implementation("io.coil-kt.coil3:coil-core:3.1.0")
//             implementation("io.coil-kt.coil3:coil-compose-core:3.1.0")
//             implementation("io.coil-kt.coil3:coil-compose:3.1.0")
//             implementation("io.coil-kt.coil3:coil-network-ktor3:3.1.0")
            // api("io.github.qdsfdhvh:image-loader:1.10.0")
            // optional - Compose Multiplatform Resources Decoder
            // api("io.github.qdsfdhvh:image-loader-extension-compose-resources:1.10.0")
            // optional - Moko Resources Decoder
            // api("io.github.qdsfdhvh:image-loader-extension-moko-resources:1.10.0")
            // optional - Blur Interceptor (only support bitmap)
            // api("io.github.qdsfdhvh:image-loader-extension-blur:1.10.0")
        }
        desktopMain.dependencies {
            // api("io.github.qdsfdhvh:image-loader-extension-imageio:1.10.0")
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
