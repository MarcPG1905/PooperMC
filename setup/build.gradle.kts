base {
    archivesName.set("${rootProject.name}-Setup")
}

tasks {
    jar {
        manifest {
            attributes["Main-Class"] = "com.marcpg.pooup.Main"
        }
    }
}
