plugins {
    id("xyz.jpenilla.run-paper") version "2.2.3"
}

base {
    archivesName.set("${rootProject.name}-Bukkit")
}

dependencies {
    implementation("com.alessiodp.libby:libby-bukkit:2.0.0-SNAPSHOT")
    implementation("net.kyori:adventure-text-minimessage:4.16.0")
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
}

tasks {
    runServer {
        minecraftVersion("1.20.4")
        downloadPlugins {
            url("https://download.luckperms.net/1544/bukkit/loader/LuckPerms-Bukkit-5.4.131.jar")
            modrinth("7IbzD4Zm", "ngDdLdWA")
        }
    }
}
