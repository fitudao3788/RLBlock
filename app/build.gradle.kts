plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "work.fitudao.rlblock"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    // 公開用署名: ~/.gradle/gradle.properties か -P で RLBLOCK_STORE_FILE / _STORE_PASSWORD / _KEY_ALIAS / _KEY_PASSWORD を指定。
    // 未指定時は debug 鍵で署名(手元ビルド用)。
    val releaseSigning = providers.gradleProperty("RLBLOCK_STORE_FILE").orNull?.let { path ->
        signingConfigs.create("release") {
            storeFile = file(path)
            storePassword = providers.gradleProperty("RLBLOCK_STORE_PASSWORD").get()
            keyAlias = providers.gradleProperty("RLBLOCK_KEY_ALIAS").get()
            keyPassword = providers.gradleProperty("RLBLOCK_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = releaseSigning ?: signingConfigs["debug"]
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.core)
    implementation(libs.activity.compose)
}
