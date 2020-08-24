import org.jetbrains.dokkaVersion
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    api("org.jetbrains:markdown:0.1.45")
    implementation(kotlin("reflect"))
    implementation("org.jsoup:jsoup:1.12.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")

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
