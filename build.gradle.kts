plugins {
    kotlin("jvm") version "1.6.20-M1"
    java
    id("maven-publish")
}

group = "com.virginiaprivacy"
version = "2.0.3"

repositories {
    mavenCentral()
}

dependencies {

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1-native-mt")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.20")
    implementation("commons-collections:commons-collections:3.2.2")
    testImplementation("io.kotest:kotest-runner-junit5:5.3.0")
    testImplementation("io.kotest:kotest-assertions-core:5.3.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
tasks.kotlinSourcesJar {
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            group = "com.virginiaprivacy"
            artifactId = "sdr"
            version = "2.0.3"
            from(components["java"])
            artifact(tasks.kotlinSourcesJar)
        }
    }
}