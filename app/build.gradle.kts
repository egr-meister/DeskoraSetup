import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * Reads signing configuration from environment variables first (used by CI),
 * then falls back to a local, git-ignored keystore.properties file.
 * Never falls back to the debug key for release artifacts.
 */
fun resolveSigning(): Map<String, String>? {
    val fromEnv = mapOf(
        "storeFile" to (System.getenv("ANDROID_KEYSTORE_FILE") ?: ""),
        "storePassword" to (System.getenv("ANDROID_KEYSTORE_PASSWORD") ?: ""),
        "keyAlias" to (System.getenv("ANDROID_KEY_ALIAS") ?: ""),
        "keyPassword" to (System.getenv("ANDROID_KEY_PASSWORD") ?: "")
    )
    if (fromEnv.values.all { it.isNotBlank() }) return fromEnv

    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) {
        val props = Properties().apply { load(FileInputStream(propsFile)) }
        val fromFile = mapOf(
            "storeFile" to (props.getProperty("storeFile") ?: ""),
            "storePassword" to (props.getProperty("storePassword") ?: ""),
            "keyAlias" to (props.getProperty("keyAlias") ?: ""),
            "keyPassword" to (props.getProperty("keyPassword") ?: "")
        )
        if (fromFile.values.all { it.isNotBlank() }) return fromFile
    }
    return null
}

android {
    namespace = "com.deskora.setup"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.deskora.setup"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    val signing = resolveSigning()

    signingConfigs {
        create("release") {
            if (signing != null) {
                storeFile = file(signing.getValue("storeFile"))
                storePassword = signing.getValue("storePassword")
                keyAlias = signing.getValue("keyAlias")
                keyPassword = signing.getValue("keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
        }
        getByName("release") {
            // Staged R8: start disabled, enable after the non-minified release is verified.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signing != null) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // Fail clearly instead of silently signing release with the debug key.
                gradle.taskGraph.whenReady {
                    val needsSigning = allTasks.any {
                        it.name.contains("Release", ignoreCase = true) &&
                            (it.name.contains("assemble", ignoreCase = true) ||
                                it.name.contains("bundle", ignoreCase = true))
                    }
                    if (needsSigning) {
                        throw GradleException(
                            "Release signing credentials are missing. Set ANDROID_KEYSTORE_FILE, " +
                                "ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD, " +
                                "or provide keystore.properties. Refusing to sign release with the debug key."
                        )
                    }
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    // Activity + Compose
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
}
