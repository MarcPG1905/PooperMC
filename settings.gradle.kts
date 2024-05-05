rootProject.name = "PooperMC"
include(
    "common",
    "setup",
    "fabric",
    "paper",
    "velocity",
)

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        mavenCentral()
        gradlePluginPortal()
    }
}
