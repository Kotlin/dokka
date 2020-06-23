import org.jetbrains.configureBintrayPublication

plugins {
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    api(project(":coreDependencies", configuration = "shadow"))

    val kotlin_version: String by project
    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlin_version")

    api("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    api("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("org.jsoup:jsoup:1.12.1")

    testImplementation(project(":testApi"))
    testImplementation(kotlin("test-junit"))
}

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
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
