import org.jetbrains.configureBintrayPublication

plugins {
    id("com.jfrog.bintray")
}

val testUtils by configurations.creating

dependencies {
    val coroutines_version: String by project
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

    api(project(":kotlin-analysis"))
    implementation("org.jsoup:jsoup:1.12.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.10")
    testImplementation(project(":test-tools"))

    testUtils(sourceSets.test.get().output)
}

task("copy_frontend", Copy::class) {
    from(File(project(":plugins:base:frontend").projectDir, "dist/"))
    destinationDir = File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/scripts")
}.dependsOn(":plugins:base:frontend:generateFrontendFiles")

tasks {
    processResources {
        dependsOn("copy_frontend")
    }

    test {
        maxHeapSize = "4G"
    }
}

publishing {
    publications {
        register<MavenPublication>("basePlugin") {
            artifactId = "dokka-base"
            from(components["java"])
        }
    }
}

configureBintrayPublication("basePlugin")