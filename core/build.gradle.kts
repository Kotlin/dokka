import org.jetbrains.dokkaVersion
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    api("org.jetbrains:markdown:0.2.4")
    implementation(kotlin("reflect"))

    val jsoup_version: String by project
    implementation("org.jsoup:jsoup:$jsoup_version")

    val jackson_version: String by project
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_version")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jackson_version")

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
