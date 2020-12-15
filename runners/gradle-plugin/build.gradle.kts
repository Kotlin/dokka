import org.jetbrains.*

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.12.0"
}

repositories {
    google()
}

dependencies {
    implementation(project(":core"))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.11.1")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("com.android.tools.build:gradle:4.0.1")
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    testImplementation(project(":test-utils"))
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    testImplementation("com.android.tools.build:gradle:4.0.1")


    constraints {
        val kotlin_version: String by project
        compileOnly("org.jetbrains.kotlin:kotlin-reflect:${kotlin_version}") {
            because("kotlin-gradle-plugin and :core both depend on this")
        }
    }
}

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

gradlePlugin {
    plugins {
        create("dokkaGradlePlugin") {
            id = "org.jetbrains.dokka"
            displayName = "Dokka plugin"
            description = "Dokka, the Kotlin documentation tool"
            implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"
            version = dokkaVersion
            isAutomatedPublishing = false
        }
    }
}

pluginBundle {
    website = "https://www.kotlinlang.org/"
    vcsUrl = "https://github.com/kotlin/dokka.git"
    tags = listOf("dokka", "kotlin", "kdoc", "android", "documentation")

    mavenCoordinates {
        groupId = "org.jetbrains.dokka"
        artifactId = "dokka-gradle-plugin"
    }
}

publishing {
    publications {
        register<MavenPublication>("dokkaGradlePluginForIntegrationTests") {
            artifactId = "dokka-gradle-plugin"
            from(components["java"])
            version = "for-integration-tests-SNAPSHOT"
        }
    }
}

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf { publication != publishing.publications["dokkaGradlePluginForIntegrationTests"] }
}

registerDokkaArtifactPublication("dokkaGradleMavenPlugin") {
    artifactId = "dokka-gradle-plugin"
}
