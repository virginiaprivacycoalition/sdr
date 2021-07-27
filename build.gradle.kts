plugins {
    kotlin("jvm") version "1.5.10"
    java
    id("maven-publish")
}

group = "com.virginiaprivacy"
version = "0.1.4"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            group = "com.virginiaprivacy.adsbtrack.drivers"
            artifactId = "sdr"
            version = "0.1.4"
            from(components["java"])
        }
    }
}