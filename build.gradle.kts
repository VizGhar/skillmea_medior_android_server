plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.firebase:firebase-admin:9.5.0")

    implementation("io.ktor:ktor-server-core:3.2.1")
    implementation("io.ktor:ktor-server-netty:3.2.1")

    implementation("io.ktor:ktor-server-content-negotiation:3.2.1")
    implementation("io.ktor:ktor-serialization-gson:3.2.1")

    implementation("com.nimbusds:nimbus-jose-jwt:10.3.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}