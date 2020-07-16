import org.jetbrains.registerDokkaArtifactPublication

plugins {
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    api("org.jetbrains:markdown:0.1.45")
    implementation(kotlin("reflect"))
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("org.jsoup:jsoup:1.12.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")

    testImplementation(project(":testApi"))
    testImplementation(kotlin("test-junit"))
}

tasks {
    processResources {
        val dokka_version: String by project
        eachFile {
            if (name == "dokka-version.properties") {
                filter { line ->
                    line.replace("<dokka-version>", dokka_version)
                }
            }
        }
    }
}

registerDokkaArtifactPublication("dokkaCore") {
    artifactId = "dokka-core"
}
