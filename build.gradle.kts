plugins {
    java
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-velocity") version "2.2.3"
}

group = "com.marcpg"
version = "1.0.1+build.4"
description = "An all-in-one solution for Server networks. Everything from administration tools, to moderation utilities and database support."
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()

    maven {
        name = "marcpg"
        url = uri("https://marcpg.com/repo/")
    }
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "sonatype-snapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    implementation("com.alessiodp.libby:libby-velocity:2.0.0-SNAPSHOT")
    implementation("com.marcpg:libpg:0.0.7")
    implementation("dev.dejvokep:boosted-yaml:1.3.2")
    implementation("org.bstats:bstats-velocity:3.0.2")
}

tasks {
    jar {
        dependsOn(shadowJar)
        enabled = false;
    }
    shadowJar {
        relocate("com.alessiodp.libby", "com.marcpg.peelocity.libs")
        relocate("dev.dejvokep.boostedyaml", "com.marcpg.peelocity.libs")
        relocate("org.bstats", "com.marcpg.peelocity.libs")

        minimize()
    }
    runVelocity {
        velocityVersion("3.3.0-SNAPSHOT")
        downloadPlugins {
            url("https://download.luckperms.net/1532/velocity/LuckPerms-Velocity-5.4.119.jar")
            modrinth("7IbzD4Zm", "eeGwpMZV")
        }
    }
}
