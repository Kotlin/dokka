/**
 * Common conventions for generating documentation with Dokkatoo.
 */

plugins {
  id("dev.adamko.dokkatoo")
}

dokka {
  dokkatooSourceSets.configureEach {
    sourceLink {
      // Read docs for more details: https://kotlinlang.org/docs/dokka-gradle.html#source-link-configuration
      remoteUrl("https://github.com/adamko-dev/dokkatoo/tree/main/examples/java-example/dokkatoo")
      localDirectory.set(rootDir)
    }
  }
}

dependencies {
  dokkatooPlugin("org.jetbrains.dokka:kotlin-as-java-plugin")
}
