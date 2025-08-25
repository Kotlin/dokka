plugins {
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.dokka") version "2.1.0-SNAPSHOT"
    id("org.jetbrains.dokka-javadoc") version "2.1.0-SNAPSHOT"
    `maven-publish`
}

version = "1.0.0"
group = "demo"


val dokkaJavadocJar by tasks.registering(Jar::class) {
    description = "A Javadoc JAR containing Dokka Javadoc"
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

val dokkaHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkaGeneratePublicationHtml.flatMap { it.outputDirectory })
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
