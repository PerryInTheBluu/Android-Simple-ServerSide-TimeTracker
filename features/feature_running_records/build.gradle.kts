import com.example.util.simpletimetracker.Base
import com.example.util.simpletimetracker.applyAndroidLibrary

plugins {
    alias(libs.plugins.gradleLibrary)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinParcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

applyAndroidLibrary()

android {
    namespace = "${Base.namespace}.feature_running_records"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":feature_dialogs:api"))
    implementation(project(":feature_running_records:api"))
    implementation(project(":feature_settings:api"))
    implementation(libs.google.dagger)
    implementation(libs.androidx.swiperefresh)
    ksp(libs.kapt.dagger)
}
