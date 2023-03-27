import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("org.jetbrains.conventions.kotlin-jvm")
    id("org.jetbrains.conventions.maven-publish")
}

dependencies {
    compileOnly(projects.core)

    implementation(kotlin("reflect"))

    implementation(libs.kotlinx.coroutines.core)

    compileOnly(projects.kotlinAnalysis)
    implementation(libs.jsoup)

    implementation(libs.jackson.kotlin)
    constraints {
        implementation(libs.jackson.databind) {
            because("CVE-2022-42003")
        }
    }

    implementation(libs.freemarker)

    testImplementation(projects.plugins.base.baseTestUtils)
    testImplementation(projects.core.contentMatcherTestUtils)

    implementation(libs.kotlinx.html)

    testImplementation(projects.kotlinAnalysis)
    testImplementation(projects.core.testApi)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
}

val projectDistDir = project(":plugins:base:frontend").file("dist")
val generateFrontendFiles = tasks.getByPath(":plugins:base:frontend:generateFrontendFiles")

val copyJsFiles by tasks.registering(Copy::class) {
    from(projectDistDir) {
        include("*.js")
    }
    dependsOn(generateFrontendFiles)
    destinationDir =
        File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/scripts")
}

val copyCssFiles by tasks.registering(Copy::class) {
    from(projectDistDir) {
        include("*.css")
    }
    dependsOn(generateFrontendFiles)
    destinationDir =
        File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/styles")
}

val copyFrontend by tasks.registering {
    dependsOn(copyJsFiles, copyCssFiles)
}

tasks {
    processResources {
        dependsOn(copyFrontend)
    }

    sourcesJar {
        dependsOn(processResources)
    }

    test {
        maxHeapSize = "4G"
    }
}

registerDokkaArtifactPublication("dokkaBase") {
    artifactId = "dokka-base"
}
