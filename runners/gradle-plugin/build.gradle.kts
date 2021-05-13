import org.jetbrains.configureBintrayPublicationIfNecessary
import org.jetbrains.configureSpacePublicationIfNecessary
import org.jetbrains.createDokkaPublishTaskIfNecessary
import org.jetbrains.dokkaVersion

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "0.10.1"
}

repositories {
    google()
}

dependencies {
    api(project(":core"))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.12.3")
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
            implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"
            version = dokkaVersion
        }
    }
}

pluginBundle {
    website = "https://www.kotlinlang.org/"
    vcsUrl = "https://github.com/kotlin/dokka.git"
    description = "Dokka, the Kotlin documentation tool"
    tags = listOf("dokka", "kotlin", "kdoc", "android")

    plugins {
        getByName("dokkaGradlePlugin") {
            displayName = "Dokka plugin"
        }
    }

    mavenCoordinates {
        groupId = "org.jetbrains.dokka"
        artifactId = "dokka-gradle-plugin"
    }
}

publishing {
    publications {
        register<MavenPublication>("pluginMaven") {
            artifactId = "dokka-gradle-plugin"
        }

        register<MavenPublication>("dokkaGradlePluginForIntegrationTests") {
            artifactId = "dokka-gradle-plugin"
            from(components["java"])
            version = "for-integration-tests-SNAPSHOT"
        }
    }
}


configureSpacePublicationIfNecessary("dokkaGradlePluginPluginMarkerMaven", "pluginMaven")
configureBintrayPublicationIfNecessary("dokkaGradlePluginPluginMarkerMaven", "pluginMaven")
createDokkaPublishTaskIfNecessary()

