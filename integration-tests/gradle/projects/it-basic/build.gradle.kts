import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.kotlinSourceSet
import java.net.URL

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))
}

tasks.withType<DokkaTask> {
    dokkaSourceSets {
        configureEach {
            moduleDisplayName.set("Basic Project")
            suppressedFiles.from(file("src/main/kotlin/it/suppressedByPath"))
            perPackageOption {
                prefix.set("it.suppressedByPackage")
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
}
