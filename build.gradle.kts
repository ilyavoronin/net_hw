import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "5.2.0"
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

tasks {
    task("buildClient", type=ShadowJar::class) {
        archiveFileName.set("client.jar")
        manifest {
            attributes(mapOf("Main-Class" to "RunClientKt"))
        }
        isZip64 = true
        from (project.configurations.runtimeClasspath.get().map {if (it.isDirectory) it else zipTree(it) })
        with(getByName("jar") as CopySpec)
    }

    task("buildServer", type=Jar::class) {
        archiveFileName.set("server.jar")
        manifest {
            attributes(mapOf("Main-Class" to "RunServerKt"))
        }
        isZip64 = true
        from (project.configurations.runtimeClasspath.get().map {if (it.isDirectory) it else zipTree(it) })
        with(getByName("jar") as CopySpec)
    }
}