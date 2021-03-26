import org.jetbrains.dokkaVersion
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    api("org.jetbrains:markdown:0.2.0")
    implementation(kotlin("reflect"))
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.11.1")

    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    testImplementation(project(":core:test-api"))
    testImplementation(kotlin("test-junit"))
}

tasks {
    processResources {
        inputs.property("dokkaVersion", dokkaVersion)
        eachFile {
            if (name == "dokka-version.properties") {
                filter { line ->
                    line.replace("<dokka-version>", dokkaVersion)
                }
            }
        }
    }
}

registerDokkaArtifactPublication("dokkaCore") {
    artifactId = "dokka-core"
}
