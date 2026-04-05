import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.jsworld.android.autolog"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.jsworld.android.autolog"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    // Core & Lifecycle
    implementation(libs.android.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)

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
    kapt(libs.androidx.hilt.compiler)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.core)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.datastore.preferences)

    // For AppWidgets support
    implementation("androidx.glance:glance-appwidget:1.1.1")
// For interop APIs with Material 3
    implementation("androidx.glance:glance-material3:1.1.1")
// For interop APIs with Material 2
    implementation("androidx.glance:glance-material:1.1.1")

    implementation(libs.androidx.work.runtime.ktx)

}

val appNameForFile = "AutoLog"
val aabTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

fun sanitizeFileName(s: String): String =
    s.replace(Regex("""[\\/:*?"<>|]"""), "_").replace(" ", "_")

tasks.register("renameReleaseAab") {
    doLast {
        // Task의 extensions가 아니라 Project의 android 확장을 가져와야 함
        val androidExt = project.extensions.findByName("android")
            ?: error("android extension not found. app 모듈(build.gradle.kts)에 있는지 확인하세요.")

        // defaultConfig 접근(리플렉션으로 안전하게)
        val defaultConfig = androidExt.javaClass.methods.first { it.name == "getDefaultConfig" }.invoke(androidExt)
        val vName = (defaultConfig.javaClass.methods.first { it.name == "getVersionName" }.invoke(defaultConfig) as? String) ?: "0.0.0"
        val vCodeAny = defaultConfig.javaClass.methods.first { it.name == "getVersionCode" }.invoke(defaultConfig)
        val vCode = (vCodeAny as? Number)?.toInt() ?: 0

        val now = LocalDateTime.now().format(aabTimeFormatter)

        val bundleRoot = layout.buildDirectory.dir("outputs/bundle").get().asFile
        val aabFile = bundleRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "aab" && it.path.contains("release", ignoreCase = true) }
            .maxByOrNull { it.lastModified() }
            ?: error("Release AAB를 찾지 못했습니다: $bundleRoot")

        val newName = sanitizeFileName("${appNameForFile}_${vName}(${vCode})_${now}.aab")
        val target = aabFile.parentFile.resolve(newName)

        if (target.exists()) target.delete()
        if (!aabFile.renameTo(target)) {
            aabFile.copyTo(target, overwrite = true)
            aabFile.delete()
        }

        println("AAB renamed: ${target.absolutePath}")
    }
}

// bundleRelease / bundleFreeRelease / bundleProdRelease 등 모든 Release 번들 작업에 붙이기
tasks.matching { it.name.startsWith("bundle") && it.name.endsWith("Release") }
    .configureEach { finalizedBy("renameReleaseAab") }