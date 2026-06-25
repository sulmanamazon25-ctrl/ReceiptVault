import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.receiptvault.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.receiptvault.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 4
        versionName = "3.1.0-alpha"

        buildConfigField("String", "SUPABASE_URL", "\"https://bkfybqwtbaecqfnzcqva.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImJrZnlicXd0YmFlY3FmbnpjcXZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODIyNTY4NTAsImV4cCI6MjA5NzgzMjg1MH0.2s7clZwzMDL7gJhlQjVNoBu1v2zDTUVJXGqqpV8gxK4\"")
        buildConfigField("String", "ACTIVATE_LICENSE_URL", "\"https://bkfybqwtbaecqfnzcqva.supabase.co/functions/v1/activate-license\"")
        buildConfigField("String", "CHECK_LICENSE_URL", "\"https://bkfybqwtbaecqfnzcqva.supabase.co/functions/v1/check-license\"")
        buildConfigField("int", "LICENSE_OFFLINE_GRACE_DAYS", "30")
        buildConfigField("int", "FREE_TIER_MAX_FOLDERS", "5")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "PRIVACY_POLICY_URL", "\"https://bkfybqwtbaecqfnzcqva.supabase.co/storage/v1/object/public/legal/privacy.html\"")
        buildConfigField("String", "TERMS_URL", "\"https://bkfybqwtbaecqfnzcqva.supabase.co/storage/v1/object/public/legal/terms.html\"")
        buildConfigField("String", "SUPPORT_EMAIL", "\"support@receiptvault.app\"")
        buildConfigField("String", "LAUNCH_PROMO_END", "\"2026-09-24\"")
        buildConfigField("String", "LICENSE_STORE_URL", "\"https://bkfybqwtbaecqfnzcqva.supabase.co/storage/v1/object/public/legal/pricing.html\"")
        buildConfigField("String", "PRICE_MONTHLY_LAUNCH", "\"$1.99\"")
        buildConfigField("String", "PRICE_YEARLY_LAUNCH", "\"$11.99\"")
        buildConfigField("String", "PRICE_LIFETIME_LAUNCH", "\"$24.99\"")
        buildConfigField("String", "PRICE_LIFETIME_LICENSE", "\"$31.99\"")
    }

    signingConfigs {
        create("release") {
            val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            if (keystorePassword != null) {
                storeFile = file(
                    System.getenv("ANDROID_KEYSTORE_FILE") ?: "${rootProject.projectDir}/release.keystore"
                )
                storePassword = keystorePassword
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (System.getenv("ANDROID_KEYSTORE_PASSWORD") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.lifecycle)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room + SQLCipher
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Security
    implementation(libs.androidx.biometric)

    // Google Play Billing (active when installed via Play Store)
    implementation(libs.billing.ktx)

    // License activation / online checks
    implementation(libs.okhttp)

    // Pro: OCR
    implementation(libs.mlkit.text.recognition)

    // Smart Scan (CameraX)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // License token cache
    implementation(libs.androidx.security.crypto)

    // Material components (XML theme parent)
    implementation(libs.google.android.material)

    // Debug tooling
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    // Instrumented tests
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
}
