plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.marcpg.poopermc"
version = "1.1.0+build.1"
description = "An all-in-one solution for servers. Everything from administration tools, to moderation utilities and database support."

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

base {
    archivesName.set("${rootProject.name}-Universal")
}

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://marcpg.com/repo/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":common"))
    // TODO: implementation(project(":bungeecord"))
    implementation(project(":paper"))
    implementation(project(":velocity"))
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.github.johnrengelman.shadow")

    version = rootProject.version

    java.sourceCompatibility = JavaVersion.VERSION_17
    java.targetCompatibility = JavaVersion.VERSION_17

    repositories {
        mavenLocal()
        mavenCentral()

        maven("https://marcpg.com/repo/")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    dependencies {
        if (project.name != "common")
            implementation(project(":common"))

        implementation("com.alessiodp.libby:libby-core:2.0.0-SNAPSHOT")
        implementation("com.marcpg:libpg:0.1.0")
        implementation("org.slf4j:slf4j-api:2.0.12")
        implementation("dev.dejvokep:boosted-yaml:1.3.2")
    }

    tasks {
        jar {
            dependsOn(shadowJar)
        }
        build {
            dependsOn(shadowJar)
        }
        shadowJar {
            relocate("com.alessiodp.libby", "com.marcpg.libs.libby")
            relocate("com.marcpg.poopermc", "com.marcpg.common")
            relocate("org.bstats", "com.marcpg.common")

            dependencies {
                exclude(dependency("net.kyori:adventure-api"))
                if (project.name != "setup") {
                    exclude(dependency("net.java.dev.jna:jna"))
                }
            }
            exclude("**/icon.png", "**/translations.properties")
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar, "copyJars")
    }
    clean {
        delete(file("build/platforms"))
    }
    shadowJar {
        relocate("org.bstats", "com.marcpg.common")
    }
}

fun outputTasks(): List<Task> {
    return listOf(
        "shadowJar",
        ":setup:shadowJar",
        // TODO: ":bungeecord:shadowJar",
        ":paper:shadowJar",
        ":velocity:shadowJar",
    ).map { tasks.findByPath(it)!! }
}

tasks.register<Copy>("copyJars") {
    outputTasks().forEach {
        from(it) {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
    into(file("build/platforms"))
    rename("(.*)-all.jar", "$1.jar")
}
