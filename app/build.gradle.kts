import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // AGP 9는 Kotlin 지원을 내장하지만 아직 kapt/KSP 와 호환되지 않는다.
    // gradle.properties 의 android.builtInKotlin=false 로 내장 Kotlin 을 끄고
    // 기존 Kotlin Gradle 플러그인 + KSP 조합을 사용한다.
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    namespace = "com.jsworld.android.autolog"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.jsworld.android.autolog"
        minSdk = 29
        targetSdk = 36
        versionCode = 14
        versionName = "1.2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("RELEASE_STORE_FILE"))
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        /**
         * R8 검증용 — release 와 동일한 축소·난독화를 별도 패키지(.qa)로 설치해
         * 실기기에서 확인한다. Play 설치본은 Google 재서명이라 로컬 릴리즈 APK 를
         * 덮어쓸 수 없어서(데이터 날리지 않고는 못 지움) 이 우회가 필요하다.
         * (릴리즈 전용 위젯 투명화 버그를 이걸로 잡았다 — InputMerger keep 참조)
         */
        create("releaseQa") {
            initWith(getByName("release"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)

            freeCompilerArgs.addAll(
                "-Xcontext-receivers"
            )
        }
    }
    buildFeatures {
        compose = true
        // BuildConfig.DEBUG 로 디버그 전용 UI(알림 테스트 버튼)를 가드하기 위해 켠다.
        buildConfig = true
    }
}

/**
 * Room 스키마를 app/schemas 에 내보낸다(`exportSchema = true` 와 짝).
 *
 * 스키마 JSON 을 git 에 두면 ⑴ 버전 간 차이를 코드 리뷰에서 볼 수 있고
 * ⑵ MigrationTestHelper 로 마이그레이션을 테스트할 수 있다.
 * DB 가 로컬에만 있는 앱이라 마이그레이션 실수는 곧 데이터 유실이므로 반드시 커밋한다.
 */
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    testImplementation(libs.junit)

    // Core & Lifecycle
    implementation(libs.android.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(libs.kotlinx.serialization.json)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.material.icons.extended)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.core)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.datastore.preferences)

    // For AppWidgets support
    implementation("androidx.glance:glance-appwidget:1.1.1")
// For interop APIs with Material 3
    implementation("androidx.glance:glance-material3:1.1.1")
// For interop APIs with Material 2
    implementation("androidx.glance:glance-material:1.1.1")

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.apache.poi.ooxml)

}