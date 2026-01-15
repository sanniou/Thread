plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeHotReload) apply false
}


allprojects {
    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(30, TimeUnit.DAYS) // Check once a month
        // Or for SNAPSHOTS specifically (dynamic versions)
        resolutionStrategy.cacheDynamicVersionsFor(30, TimeUnit.DAYS)
    }
}


