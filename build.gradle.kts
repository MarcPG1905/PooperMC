plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.marcpg.poopermc"
version = "1.1.3+build.1"
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
        implementation("dev.dejvokep:boosted-yaml:1.3.2")
        implementation("com.marcpg:libpg:0.1.1")
        implementation("com.google.code.gson:gson:2.11.0")
        if (project.name != "common")
            implementation(project(":common"))
        if (project.name != "setup")
            implementation("com.alessiodp.libby:libby-core:2.0.0-SNAPSHOT")
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
            relocate("dev.dejvokep.boostedyaml", "com.marcpg.libs.boostedyaml")
            relocate("com.marcpg.poopermc", "com.marcpg.common")
            relocate("org.bstats", "com.marcpg.common")

            exclude("**/icon.png", "**/translations.properties", "net/kyori/**")
            if (project.name != "setup") exclude("com/sun/jna/**")
        }
        processResources {
            filter {
                it.replace("\${version}", version.toString())
            }
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
        relocate("com.alessiodp.libby", "com.marcpg.libs.libby")
        relocate("dev.dejvokep.boostedyaml", "com.marcpg.libs.boostedyaml")
        relocate("org.bstats", "com.marcpg.common")

        exclude("**/icon.png", "com/sun/jna/**", "net/kyori/**")
    }
}

fun outputTasks(): List<Task> {
    return listOf(
        "shadowJar",
        ":setup:shadowJar",
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
