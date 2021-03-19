plugins {
    java
    id("application")
    kotlin("jvm") version "1.4.30"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktor_version = "1.5.2"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("com.athaydes.rawhttp:rawhttp-core:2.4.1")
    testCompile("junit", "junit", "4.12")
}

application {
    mainClassName = "RunServerKt"
}
