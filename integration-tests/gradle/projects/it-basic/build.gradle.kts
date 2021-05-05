import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.kotlinSourceSet
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:${System.getenv("DOKKA_VERSION")}")
    }
}

version = "1.5-SNAPSHOT"

apply(from = "../template.root.gradle.kts")

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))
}

tasks.withType<DokkaTask> {
    moduleName.set("Basic Project")
    dokkaSourceSets {
        configureEach {
            suppressedFiles.from(file("src/main/kotlin/it/suppressedByPath"))
            perPackageOption {
                matchingRegex.set("it.suppressedByPackage.*")
                suppress.set(true)
            }
            sourceLink {
                localDirectory.set(file("src/main"))
                remoteUrl.set(
                    URL(
                        "https://github.com/Kotlin/dokka/tree/master/" +
                                "integration-tests/gradle/projects/it-basic/src/main"
                    )
                )
            }
        }

        register("myTest") {
            kotlinSourceSet(kotlin.sourceSets["test"])
        }
    }
    suppressObviousFunctions.set(false)

    pluginsMapConfiguration.set(mapOf(DokkaBase::class.qualifiedName to """{ "customStyleSheets": ["${file("../customResources/logo-styles.css")}", "${file("../customResources/custom-style-to-add.css")}"], "customAssets" : ["${file("../customResources/custom-resource.svg")}"] }"""))
}
