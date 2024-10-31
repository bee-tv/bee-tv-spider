plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "bee-tv-spider"
include("spider-api")
include("spider-impl")
