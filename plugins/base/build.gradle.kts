import org.jetbrains.configureBintrayPublication

plugins {
    id("com.jfrog.bintray")
}

dependencies {

    val kotlin_version: String by project
    api("org.jetbrains.kotlin:kotlin-compiler:$kotlin_version")

    implementation("org.jsoup:jsoup:1.12.1")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.10")
    testImplementation(project(":test-tools"))
}

task("copy_frontend", Copy::class) {
    from(File(project(":plugins:base:frontend").projectDir, "dist/"))
    destinationDir = File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/scripts")
}.dependsOn(":plugins:base:frontend:generateFrontendFiles")

tasks {
    processResources {
        dependsOn("copy_frontend")
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
