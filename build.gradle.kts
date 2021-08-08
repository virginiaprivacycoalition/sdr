plugins {
    kotlin("jvm") version "1.5.30-M1"
    java
    id("maven-publish")
}

group = "com.virginiaprivacy"
version = "0.1.8"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1-native-mt")
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
            version = "0.1.8"
            from(components["java"])
        }
    }
}