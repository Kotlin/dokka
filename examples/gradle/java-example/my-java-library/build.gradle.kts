plugins {
  `java-library-convention`
  `dokka-convention`
}

dokka {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }
}
