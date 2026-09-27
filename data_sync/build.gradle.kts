import com.example.util.simpletimetracker.Base
import com.example.util.simpletimetracker.applyAndroidLibrary

plugins {
    alias(libs.plugins.gradleLibrary)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

applyAndroidLibrary()

android {
    namespace = "${Base.namespace}.data_sync"

    defaultConfig {
        // HTTP server URLs are only accepted in the debug build variant.
        // Release builds must use HTTPS.
        buildConfigField(
            "boolean",
            "ALLOW_HTTP_SERVER_URL",
            "false",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField(
                "boolean",
                "ALLOW_HTTP_SERVER_URL",
                "true",
            )
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data_local"))
    implementation(libs.androidx.room)
    implementation(libs.ktx.room)
    ksp(libs.kapt.room)
    api(libs.androidx.work)
    implementation(libs.androidx.hiltWork)
    ksp(libs.androidx.hiltWorkCompiler)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofitMoshi)
    implementation(libs.squareup.moshi)
    implementation(libs.androidx.security)
    ksp(libs.kapt.dagger)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockito)
    testImplementation(libs.test.mockitoKotlin)
    testImplementation(libs.test.coroutines)
}
