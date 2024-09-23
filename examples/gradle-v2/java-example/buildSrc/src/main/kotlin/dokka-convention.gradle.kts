/**
 * Common conventions for generating documentation with Dokka.
 */

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            // Read docs for more details: https://kotlinlang.org/docs/dokka-gradle.html#source-link-configuration
            remoteUrl("https://github.com/Kotlin/dokka/tree/master/examples/gradle/java-example")
            localDirectory.set(rootDir)
        }
    }
}

dependencies {
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin")
}
