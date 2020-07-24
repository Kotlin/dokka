import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication

plugins {
    id("com.github.johnrengelman.shadow")
    id("com.jfrog.bintray")
}

repositories {
    val use_redirector_enabled = System.getenv("TEAMCITY_VERSION") != null || run {
        val cache_redirector_enabled: String? by project
        cache_redirector_enabled == "true"
    }
    if (use_redirector_enabled) {
        maven(url = "https://cache-redirector.jetbrains.com/dl.bintray.com/kotlin/kotlinx")
    } else {
        maven(url = "https://dl.bintray.com/kotlin/kotlinx")
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.2.1")
    implementation(project(":core"))
    implementation(kotlin("stdlib"))
}

tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-cli-$dokka_version.jar")
        archiveClassifier.set("")
        manifest {
            attributes("Main-Class" to "org.jetbrains.dokka.MainKt")
        }
    }
}

registerDokkaArtifactPublication("dokkaCli"){
    artifactId = "dokka-cli"
    component = Shadow
}

