import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("com.jfrog.bintray")
}

dependencies {
    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    api(project(":kotlin-analysis"))
    implementation("org.jsoup:jsoup:1.13.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))

    val kotlinx_html_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
}

val projectDistDir = project(":plugins:base:frontend").file("dist")
val generateFrontendFiles = tasks.getByPath(":plugins:base:frontend:generateFrontendFiles")

val copyJsFiles by tasks.registering(Copy::class){
    from(projectDistDir){
        include("*.js")
    }
    dependsOn(generateFrontendFiles)
    destinationDir = File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/scripts")
}

val copyCssFiles by tasks.registering(Copy::class){
    from(projectDistDir){
        include("*.css")
    }
    dependsOn(generateFrontendFiles)
    destinationDir = File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/styles")
}

val copyFrontend by tasks.registering {
    dependsOn(copyJsFiles, copyCssFiles)
}

tasks {
    processResources {
        dependsOn(copyFrontend)
    }

    test {
        maxHeapSize = "4G"
    }
}

registerDokkaArtifactPublication("dokkaBase") {
    artifactId = "dokka-base"
}
