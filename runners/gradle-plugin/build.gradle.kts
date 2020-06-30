import org.jetbrains.configureBintrayPublication

plugins {
    id("com.gradle.plugin-publish")
    `java-gradle-plugin`
}

repositories {
    google()
}

dependencies {
    implementation(project(":core"))
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    compileOnly("com.android.tools.build:gradle:3.0.0")
    compileOnly("com.android.tools.build:gradle-core:3.0.0")
    compileOnly("com.android.tools.build:builder-model:3.0.0")
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    testImplementation(gradleApi())
    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin")

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

// TODO: https://github.com/Kotlin/dokka/issues/1052
tasks.getByName("validatePlugins").enabled = false

val gradlePluginImplementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"

publishing {
    publications {
        register<MavenPublication>("dokkaGradlePlugin") {
            artifactId = "dokka-gradle-plugin"
            from(components["java"])
            artifact(sourceJar.get())
        }

        register<MavenPublication>("dokkaGradlePluginForIntegrationTests") {
            artifactId = "dokka-gradle-plugin"
            from(components["java"])
            artifact(sourceJar.get())
            version = "for-integration-tests-SNAPSHOT"
        }
    }
}

configureBintrayPublication("dokkaGradlePlugin")

gradlePlugin {
    plugins {
        create("dokkaGradlePlugin") {
            id = "org.jetbrains.dokka"
            implementationClass = gradlePluginImplementationClass
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
