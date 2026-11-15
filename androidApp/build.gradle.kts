import groovy.json.JsonSlurper
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

val firebaseOptions: Map<String, String> =
    file("google-services.json")
        .takeIf { it.exists() }
        ?.let { servicesFile ->
            val services = JsonSlurper().parse(servicesFile) as Map<*, *>
            val project = services["project_info"] as Map<*, *>
            val clients = (services["client"] as List<*>).map { it as Map<*, *> }
            val client =
                clients.firstOrNull { client ->
                    val info = (client["client_info"] as Map<*, *>)["android_client_info"] as Map<*, *>
                    info["package_name"] == "net.neednot.renttracker"
                } ?: clients.first()
            mapOf(
                "FIREBASE_APP_ID" to (client["client_info"] as Map<*, *>)["mobilesdk_app_id"].toString(),
                "FIREBASE_API_KEY" to ((client["api_key"] as List<*>).first() as Map<*, *>)["current_key"].toString(),
                "FIREBASE_PROJECT_ID" to project["project_id"].toString(),
                "FIREBASE_SENDER_ID" to project["project_number"].toString(),
            )
        }.orEmpty()

android {
    namespace = "net.neednot.renttracker"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "net.neednot.renttracker"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "CONVEX_URL", "\"${localProperties.getProperty("convex.url", "")}\"")
        buildConfigField("String", "AUTH_URL", "\"${localProperties.getProperty("auth.url", "")}\"")
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProperties.getProperty("google.webClientId", "")}\"",
        )
        listOf("FIREBASE_APP_ID", "FIREBASE_API_KEY", "FIREBASE_PROJECT_ID", "FIREBASE_SENDER_ID").forEach { name ->
            buildConfigField("String", name, "\"${firebaseOptions[name].orEmpty()}\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.shared)
    implementation(libs.androidx.activity.compose)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.convex.android)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.work.runtime)
    implementation(libs.firebase.messaging)
}
