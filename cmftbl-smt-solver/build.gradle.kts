plugins {
    id("buildlogic.kotlin-library-conventions")
    kotlin("plugin.serialization") version "2.0.20"
}

dependencies {
    implementation("tools.aqua:stars-core:0.5")
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}