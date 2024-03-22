plugins {
    id("xyz.jpenilla.run-velocity") version "2.2.3"
}

base {
    archivesName.set("${rootProject.name}-Velocity")
}

dependencies {
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    implementation("com.alessiodp.libby:libby-velocity:2.0.0-SNAPSHOT")
    implementation("org.bstats:bstats-velocity:3.0.2")
}

tasks {
    runVelocity {
        velocityVersion("3.3.0-SNAPSHOT")
        downloadPlugins {
            url("https://download.luckperms.net/1534/velocity/LuckPerms-Velocity-5.4.121.jar")
            modrinth("7IbzD4Zm", "eeGwpMZV")
        }
    }
}
