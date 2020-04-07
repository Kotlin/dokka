import org.jetbrains.configureBintrayPublication

plugins {
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    api(project(":coreDependencies", configuration = "shadow"))

    val kotlin_version: String by project
    api("org.jetbrains.kotlin:kotlin-compiler:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("org.jsoup:jsoup:1.12.1")

    testImplementation(project(":testApi"))
    testImplementation("junit:junit:4.13")
}

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

task("copy_search", Copy::class) {
    from(File(project(":core:search-component").projectDir, "dist/"))
    destinationDir = File(sourceSets.main.get().resources.sourceDirectories.singleFile, "dokka/scripts")
}.dependsOn(":core:search-component:generateSearchFiles")

tasks {
    processResources {
        dependsOn("copy_search")
    }
}

publishing {
    publications {
        register<MavenPublication>("dokkaCore") {
            artifactId = "dokka-core"
            from(components["java"])
            artifact(sourceJar.get())
        }
    }
}

configureBintrayPublication("dokkaCore")
