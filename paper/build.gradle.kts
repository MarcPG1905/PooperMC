plugins {
    id("xyz.jpenilla.run-paper") version "2.2.3"
}

base {
    archivesName.set("${rootProject.name}-Bukkit")
}

dependencies {
    implementation("com.alessiodp.libby:libby-bukkit:2.0.0-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
}

tasks {
    runServer {
        minecraftVersion("1.20.4")
        downloadPlugins {
            url("https://download.luckperms.net/1534/bukkit/loader/LuckPerms-Bukkit-5.4.121.jar")
        }
    }
}
