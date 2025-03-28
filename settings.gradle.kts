plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "bachelor-thesis-project"
include("cmftbl-smt-solver")
include("stars-data-av")
include("experiments")
