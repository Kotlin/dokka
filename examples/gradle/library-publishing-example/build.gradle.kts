plugins {
    kotlin("jvm") version "1.9.23"
    id("dev.adamko.dokkatoo") version "2.4.0-SNAPSHOT"
    `maven-publish`
}

version = "1.0.0"
group = "demo"

val dokkatooJavadocJar by tasks.registering(Jar::class) {
    description = "A Javadoc JAR containing Dokka Javadoc"
    from(tasks.dokkatooGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

val dokkatooHtmlJar by tasks.registering(Jar::class) {
    description = "A HTML Documentation JAR containing Dokka HTML"
    from(tasks.dokkatooGeneratePublicationHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-doc")
}

publishing {
    publications {
        register<MavenPublication>("library") {
            from(components["java"])
            artifact(dokkatooJavadocJar)
            artifact(dokkatooHtmlJar)
        }
    }

    // For verification, run `gradle publishAllToDevMaven` and check ./build/dev-maven`
    repositories {
        maven(file("build/dev-maven")) {
            name = "DevMaven"
        }
    }
}
