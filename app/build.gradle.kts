plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.jexpop.appkotlininggas"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jexpop.appkotlininggas"
        minSdk = 24
        targetSdk = 36
        versionCode = 16
        versionName = "1.0.16"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = org.jetbrains.kotlin.konan.properties.loadProperties(
            rootProject.file("local.properties").absolutePath
        )
        buildConfigField("String", "SUPABASE_URL", "\"${properties["SUPABASE_URL"]}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${properties["SUPABASE_KEY"]}\"")
        buildConfigField("String", "ADMIN_EMAIL", "\"${properties["ADMIN_EMAIL"]}\"")
        buildConfigField("String", "DRIVE_ALLOWED_EMAIL", "\"${properties["DRIVE_ALLOWED_EMAIL"] ?: properties["ADMIN_EMAIL"]}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        applicationVariants.all {
            outputs.all {
                val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
                output.outputFileName = "AppKotlinIngGas-${versionName}-${buildType.name}.apk"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.1.4"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // Google Auth para Supabase
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("androidx.browser:browser:1.8.0")

    // Google Drive API
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.api-client:google-api-client-android:2.2.0")
    implementation("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")

    // Motor HTTP requerido por Supabase
    implementation("io.ktor:ktor-client-android:3.1.3")

    // Corrutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Serialización (para mapear tablas a data classes)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ViewModel con Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Compose Runtime
    implementation("androidx.compose.runtime:runtime:1.7.8")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Seguridad para almacenamiento cifrado
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

}
