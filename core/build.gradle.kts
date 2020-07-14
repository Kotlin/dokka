import org.jetbrains.configurePublication

plugins {
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    api(project("dependencies", configuration = "shadow"))

    implementation(kotlin("reflect"))
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("org.jsoup:jsoup:1.12.1")

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

configurePublication("dokka-core")
