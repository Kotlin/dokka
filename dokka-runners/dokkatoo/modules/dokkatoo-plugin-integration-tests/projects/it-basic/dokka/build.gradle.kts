import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.kotlinSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.DokkaConfiguration
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.9.0")
    }
}

version = "1.9.0-SNAPSHOT"

apply(from = "./template.root.gradle.kts")

dependencies {
    testImplementation(kotlin("test-junit"))
}

tasks.withType<DokkaTask> {
    moduleName = "Basic Project"
    dokkaSourceSets {
        configureEach {
            documentedVisibilities =
                setOf(DokkaConfiguration.Visibility.PUBLIC, DokkaConfiguration.Visibility.PROTECTED)
            suppressedFiles.from(file("src/main/kotlin/it/suppressedByPath"))
            perPackageOption {
                matchingRegex = "it.suppressedByPackage.*"
                suppress = true
            }
            perPackageOption {
                matchingRegex = "it.overriddenVisibility.*"
                documentedVisibilities = setOf(DokkaConfiguration.Visibility.PRIVATE)
            }
            sourceLink {
                localDirectory = file("src/main")
                remoteUrl =
                    URL(
                        "https://github.com/Kotlin/dokka/tree/master/" +
                                "integration-tests/gradle/projects/it-basic/src/main"
                    )
            }
        }

        register("myTest") {
            kotlinSourceSet(kotlin.sourceSets["test"])
        }
    }
    suppressObviousFunctions = false

    pluginsMapConfiguration = mapOf(DokkaBase::class.qualifiedName to """{ "customStyleSheets": ["${file("../customResources/logo-styles.css").invariantSeparatorsPath}", "${file("../customResources/custom-style-to-add.css").invariantSeparatorsPath}"], "customAssets" : ["${file("../customResources/custom-resource.svg").invariantSeparatorsPath}"] }""")
}
