plugins {
    kotlin("jvm") version "1.6.0"
    java
    id("maven-publish")
}

group = "com.virginiaprivacy"
version = "0.3.1"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            group = "com.virginiaprivacy.adsbtrack.drivers"
            artifactId = "sdr"
            version = "0.3.1"
            from(components["java"])
        }
    }
}