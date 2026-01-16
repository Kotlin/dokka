plugins {
    kotlin("jvm") version "2.3.10-RC"
    id("org.jetbrains.dokka") version "2.1.0"
    id("org.jetbrains.dokka-javadoc") version "2.1.0"
    `maven-publish`
}

version = "1.0.0"
group = "demo"


val dokkaJavadocJar by tasks.registering(Jar::class) {
    description = "A Javadoc JAR containing Dokka Javadoc"
    from(tasks.dokkaGeneratePublicationJavadoc)
    archiveClassifier.set("javadoc")
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml)
    archiveClassifier.set("html-doc")
}

publishing {
    publications {
        register<MavenPublication>("library") {
            from(components["java"])
            artifact(dokkaJavadocJar)
            artifact(dokkaHtmlJar)
        }
    }

    // For verification, run `gradle publishAllToDevMaven` and check ./build/dev-maven`
    repositories {
        maven(file("build/dev-maven")) {
            name = "DevMaven"
        }
    }
}
