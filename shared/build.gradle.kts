plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    compilerOptions {
        // Room generates an `actual object` for the database constructor.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "net.neednot.renttracker.shared"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        androidResources { enable = true }
        withHostTest { isIncludeAndroidResources = true }
    }

    // Host-only target: runs the JVM tests (architecture, Room). No app ships on it.
    jvm()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            api(libs.kotlin.result)
            implementation(libs.kotlin.result.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
            implementation(libs.better.auth)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
        jvmTest.dependencies {
            implementation(libs.konsist)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
            implementation(libs.roborazzi.junit.rule)
            implementation(libs.androidx.compose.ui.test.junit4)
            implementation(libs.androidx.compose.ui.test.manifest)
            implementation(libs.androidx.test.core)
        }
    }
}

compose.resources {
    packageOfResClass = "net.neednot.renttracker.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64").forEach { configuration ->
        add(configuration, libs.androidx.room.compiler)
    }
}

val recordScreenshots = providers.gradleProperty("recordScreenshots").isPresent

tasks.withType<Test>().configureEach {
    if (name == "testAndroidHostTest") {
        systemProperty("roborazzi.test.record", recordScreenshots)
        systemProperty("roborazzi.test.verify", !recordScreenshots)
        inputs.dir("src/androidHostTest/screenshots").withPropertyName("goldenScreenshots")
    }
}
