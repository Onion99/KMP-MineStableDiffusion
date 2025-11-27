import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("android-library-convention")
    id("kotlin-library-convention")
    alias(libs.plugins.ktor)
}
android {
    namespace = "com.onion.data.network"
    buildFeatures {
        compose = false
    }
}

kotlin {

    sourceSets {
        named("commonMain") {
            dependencies {
                // ---- di ------
                implementation(libs.koin.core)
                // ---- ktor ------
                api(libs.ktor.core)
                api(libs.ktor.client.contentNegotiation)
                api(libs.ktor.client.contentNegotiation)
                api(libs.ktor.client.logging)
                api(libs.ktor.client.resources)
                api(libs.ktor.serialization.kotlinx.json)
                // ---- sandwich ------
                api(libs.sandwich)
                api(libs.sandwich.ktor)
                api(libs.sandwich.ktorfit)
                api(projects.dataModel)
            }
        }
        named("androidMain") {
            dependencies {
                api(libs.ktor.client.okhttp)
            }
        }
        desktopMain{
            dependencies {
                api(libs.ktor.client.okhttp)
            }
        }
    }
}

