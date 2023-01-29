import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.0.0"
    kotlin("plugin.serialization") version embeddedKotlinVersion
}

group = "org.jetbrains.dokka"
version = "2.0.0"

dependencies {
    implementation("org.jetbrains.dokka:dokka-core:1.7.20")

    compileOnly("com.android.tools.build:gradle:4.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    testImplementation("com.android.tools.build:gradle:4.0.1")
}

java {
    withSourcesJar()
}

gradlePlugin {
    plugins.create("dokkaGradlePlugin2") {
        id = "org.jetbrains.dokka2"
        displayName = "Dokka plugin 2"
        description = "Dokka, the Kotlin documentation tool"
        implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"
        isAutomatedPublishing = true
    }
}

pluginBundle {
    website = "https://www.kotlinlang.org/"
    vcsUrl = "https://github.com/kotlin/dokka.git"
    tags = listOf("dokka", "kotlin", "kdoc", "android", "documentation")
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        this.freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}
