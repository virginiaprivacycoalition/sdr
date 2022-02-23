plugins {
    kotlin("jvm") version "1.6.20-M1"
    java
    id("maven-publish")
}

group = "com.github.virginiaprivacycoalition"
version = "0.7.2"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
    testImplementation("io.kotest:kotest-runner-junit5:5.0.3")
    testImplementation("io.kotest:kotest-assertions-core:5.0.3")
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
            group = "com.github.virginiaprivacycoalition"
            artifactId = "sdr"
            version = "0.7.2"
            from(components["java"])
            artifact(tasks.kotlinSourcesJar)
        }
    }
}