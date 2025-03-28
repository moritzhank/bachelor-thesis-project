plugins {
    id("buildlogic.kotlin-library-conventions")
    kotlin("plugin.serialization") version "2.0.20"
}

dependencies {
    implementation("tools.aqua:stars-core:0.5")
    implementation(project(":cmftbl-smt-solver"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
}
