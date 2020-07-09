import org.jetbrains.configureBintrayPublication
import org.jetbrains.dokkaVersion

plugins {
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
    testImplementation(gradleKotlinDsl())
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

gradlePlugin {
    plugins {
        create("dokkaGradlePlugin") {
            id = "org.jetbrains.dokka"
            implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"
            version = dokkaVersion
        }
    }
}

publishing {
    publications {
        maybeCreate<MavenPublication>("pluginMaven").apply {
            artifactId = "dokka-gradle-plugin"
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


configureBintrayPublication("dokkaGradlePluginPluginMarkerMaven", "pluginMaven")
