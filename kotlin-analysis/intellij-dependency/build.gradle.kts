import org.jetbrains.DokkaPublicationBuilder.Component.Shadow
import org.jetbrains.registerDokkaArtifactPublication
import java.net.URL

plugins {
    id("com.github.johnrengelman.shadow")
    `maven-publish`
    id("com.jfrog.bintray")
}

repositories {
    mavenCentral()
    maven(url = "https://www.jetbrains.com/intellij-repository/snapshots")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide")
    maven("https://kotlin.bintray.com/kotlin-ide-plugin-dependencies")
    maven("https://jetbrains.bintray.com/intellij-third-party-dependencies")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

val intellijCore: Configuration by configurations.creating

fun intellijCoreAnalysis() = zipTree(intellijCore.singleFile).matching {
    include("intellij-core-analysis-deprecated.jar")
}

dependencies {
    val kotlin_plugin_version: String by project
    api("org.jetbrains.kotlin:common:$kotlin_plugin_version")
    api("org.jetbrains.kotlin:idea:$kotlin_plugin_version") {
        isTransitive = false
    }
    api("org.jetbrains.kotlin:core:$kotlin_plugin_version")
    api("org.jetbrains.kotlin:native:$kotlin_plugin_version")

    val idea_version: String by project
    intellijCore("com.jetbrains.intellij.idea:intellij-core:$idea_version")
    implementation(intellijCoreAnalysis())
}

tasks {
    val getDependenciesUrls by creating {
        project.repositories.forEach { repository ->
            val dependencyGroup = "org.jetbrains.intellij.deps"
            val dependencyName = "log4j"
            val dependencyVersion = "1.2.17" //1.2.17.1
            val url = (repository as MavenArtifactRepository).url.let {
                if (!it.toString().endsWith("/")) {
                    "$it/"
                } else {
                    it
                }
            }
            val jarUrl = String.format(
                "%s%s/%s/%s/%s-%s.jar", url,
                dependencyGroup.replace('.', '/'), dependencyName, dependencyVersion,
                dependencyName, dependencyVersion
            )

            kotlin.runCatching {
                val jarFile = URL(jarUrl)
                val inStream = jarFile.openStream();
                if (inStream != null) {
                    println(
                        String.format("%s:%s:%s", dependencyGroup, dependencyName, dependencyVersion)
                                + " -> " + jarUrl
                    )
                }
            }
        }
    }

    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-kotlin-analysis-intellij-$dokka_version.jar")
        archiveClassifier.set("")

        exclude("colorScheme/**")
        exclude("fileTemplates/**")
        exclude("inspectionDescriptions/**")
        exclude("intentionDescriptions/**")
        exclude("tips/**")
        exclude("messages/**")
        exclude("src/**")
        exclude("**/*.kotlin_metadata")
        exclude("**/*.kotlin_builtins")
    }
}

registerDokkaArtifactPublication("kotlinAnalysisIntelliJ") {
    artifactId = "kotlin-analysis-intellij"
    component = Shadow
}
