import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Token'lar gradle property / env'den gelir (CI: -PMAPBOX_PUBLIC_TOKEN=... -PHOODER_APP_KEY=...)
fun secret(name: String, def: String = ""): String =
    (project.findProperty(name) as String?) ?: System.getenv(name) ?: def

android {
    namespace = "app.realvirtuality.landlord"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.realvirtuality.landlord"
        minSdk = 24
        targetSdk = 35
        versionCode = secret("VERSION_CODE", "1100").toInt()
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "MAPBOX_TOKEN", "\"${secret("MAPBOX_PUBLIC_TOKEN", "pk.placeholder")}\"")
        buildConfigField("String", "HOODER_APP_KEY", "\"${secret("HOODER_APP_KEY")}\"")
        buildConfigField("String", "API_BASE", "\"https://realvirtuality.app/hooder-api\"")
    }

    androidResources {
        localeFilters += listOf("en","tr","es","fr","de","it","pt","ru","ar","zh","ja","ko","uk","hi","az","fa")
    }

    signingConfigs {
        create("release") {
            val ks = file("keystore.jks")
            if (ks.exists()) {
                val props = Properties().apply {
                    file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
                }
                storeFile = ks
                storePassword = props.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD")
                keyAlias = props.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS")
                keyPassword = props.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("debug") { applicationIdSuffix = ".debug" }
        getByName("release") {
            isMinifyEnabled = false
            if (file("keystore.jks").exists()) signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.coroutines.android)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.datastore.preferences)
    implementation(libs.billing.ktx)
    implementation(libs.mapbox.maps)
    implementation(libs.mapbox.compose)
    implementation(libs.play.integrity)
    implementation(libs.play.location)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
