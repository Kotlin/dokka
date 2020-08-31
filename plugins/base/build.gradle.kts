import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("com.jfrog.bintray")
}

dependencies {
    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    api(project(":kotlin-analysis"))
    implementation("org.jsoup:jsoup:1.12.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.1")
    testImplementation(project(":plugins:base:base-test-utils"))
    testImplementation(project(":core:content-matcher-test-utils"))

    val kotlinx_html_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:$kotlinx_html_version")
}

val projectDistDir = File(project(":plugins:base:frontend").projectDir, "dist/")

val copyJsFiles by tasks.registering(Copy::class){
    from(projectDistDir){
        include("*.js")
    }
    destinationDir = File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/scripts")
}

val copyCssFiles by tasks.registering(Copy::class){
    from(projectDistDir){
        include("*.css")
    }
    destinationDir = File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/styles")
}

task("copy_frontend") {
    val generate = tasks.getByPath(":plugins:base:frontend:generateFrontendFiles")
    setDependsOn(listOf(generate, copyJsFiles, copyCssFiles))
    copyJsFiles.get().mustRunAfter(generate)
    copyCssFiles.get().mustRunAfter(generate)
}

tasks {
    processResources {
        dependsOn("copy_frontend")
    }

    test {
        maxHeapSize = "4G"
    }
}

registerDokkaArtifactPublication("dokkaBase") {
    artifactId = "dokka-base"
}
