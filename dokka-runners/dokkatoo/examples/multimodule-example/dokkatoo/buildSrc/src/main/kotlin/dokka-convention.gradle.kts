/**
 * Common conventions for generating documentation with Dokkatoo.
 */

plugins {
  id("org.jetbrains.dokka.dokkatoo")
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    sourceLink {
      // Read docs for more details: https://kotlinlang.org/docs/dokka-gradle.html#source-link-configuration
      remoteUrl("https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example")
      localDirectory.set(rootDir)
    }
  }
}
