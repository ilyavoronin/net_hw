plugins {
    java
    id("application")
    kotlin("jvm") version "1.4.30"
}

group = "org.example"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

repositories {
    mavenCentral()
}

val ktor_version = "1.5.2"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("com.athaydes.rawhttp:rawhttp-core:2.4.1")
    implementation("com.sksamuel.hoplite:hoplite-core:1.3.14")
    implementation("com.sksamuel.hoplite:hoplite-yaml:1.3.14")
    testCompile("junit", "junit", "4.12")
}

application {
    mainClassName = "RunServerKt"
}
