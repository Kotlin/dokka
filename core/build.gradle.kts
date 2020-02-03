import org.jetbrains.configureBintrayPublication

plugins {
    id("com.github.johnrengelman.shadow")
    `maven-publish`
    id("com.jfrog.bintray")
}

dependencies {
    implementation(project(":coreDependencies", configuration = "shadow"))

    val kotlin_version: String by project
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-script-runtime:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("org.jsoup:jsoup:1.12.1")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("org.jetbrains:markdown:0.1.40")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.10")

    testImplementation(project(":testApi"))
    testImplementation("junit:junit:4.13")
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-core-$dokka_version.jar")
        archiveClassifier.set("")
    }
}

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    publications {
        register<MavenPublication>("dokkaCore") {
            artifactId = "dokka-core"
            project.shadow.component(this)
            artifact(sourceJar.get())
        }
    }
}

configureBintrayPublication("dokkaCore")
