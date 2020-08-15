import org.jetbrains.configureBintrayPublication
import org.jetbrains.dokkaVersion

plugins {
    `java-gradle-plugin`
    id("com.gradle.plugin-publish")
}

repositories {
    google()
}

dependencies {
    implementation(project(":core"))
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.11.1")
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("com.android.tools.build:gradle:3.0.0")
    compileOnly("com.android.tools.build:gradle-core:3.0.0")
    compileOnly("com.android.tools.build:builder-model:3.0.0")
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    testImplementation("com.android.tools.build:gradle:3.0.0")


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


configureBintrayPublication("dokkaGradlePluginPluginMarkerMaven", "pluginMaven")
