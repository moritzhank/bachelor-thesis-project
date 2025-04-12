plugins {
    id("buildlogic.kotlin-application-conventions")
    kotlin("plugin.serialization") version "2.0.20"
}

val experimentClass = "ChangedLaneAndNoRollBeforeIncrementalExperimentKt"

application {
    mainClass.set("de.haneke.moritz.cmftbl.smt.solver.experiments.$experimentClass")
}

tasks.jar {
    archiveFileName = "${archiveBaseName.get()}.jar"
}

tasks.distZip {
    archiveFileName.set("${experimentClass.dropLast(2)}.zip")
    destinationDirectory.set(rootProject.file("_experiment${File.separator}"))
    into("${experimentClass.dropLast(2)}/bin/") {
        from("src/main/resources/")
        include("*/*")
    }
}

tasks.clean {
    delete("../_experiment/")
    delete("../_smtTmp/")
    delete("../_formulaSvgs/")
    delete("../_treeSvgs/")
}

dependencies {
    implementation("tools.aqua:stars-core:0.5")
    implementation("tools.aqua:stars-logic-kcmftbl:0.5")
    implementation("tools.aqua:stars-importer-carla:0.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("com.github.oshi:oshi-core:6.6.5")
    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    implementation(project(":stars-data-av"))
    implementation(project(":cmftbl-smt-solver"))
}