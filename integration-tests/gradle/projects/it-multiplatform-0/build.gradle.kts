import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

kotlin {
    jvm()
    linuxX64("linux")
    macosX64("macos")
    js()
    sourceSets {
        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
            }
        }
    }
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        configureEach {
            externalDocumentationLink {
                url.set(URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/"))
                //packageListUrl.set(URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/package-list"))
            }
        }
    }
}
