import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("com.github.johnrengelman.shadow")
    `maven-publish`
    id("com.jfrog.bintray")
}

repositories {
    maven(url = "https://kotlin.bintray.com/kotlin-plugin")
}

dependencies {
    // TODO (see https://github.com/Kotlin/dokka/issues/1009)
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("org.jetbrains:markdown:0.1.41") {
        because("it's published only on bintray")
    }
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-dependencies-$dokka_version.jar")
        archiveClassifier.set("")
    }
}

registerDokkaArtifactPublication("dokkaCoreDependencies") {
    artifactId = "dokka-core-dependencies"
    component = Shadow
}
