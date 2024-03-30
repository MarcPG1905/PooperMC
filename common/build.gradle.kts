plugins {
    `java-library`
}

dependencies {
    implementation("net.kyori:adventure-text-serializer-ansi:4.16.0")
    compileOnly("org.slf4j:slf4j-api:2.0.12")
    compileOnly("net.kyori:adventure-text-minimessage:4.16.0")
}
