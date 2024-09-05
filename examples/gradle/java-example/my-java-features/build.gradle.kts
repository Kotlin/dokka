plugins {
  `java-mongodb-library-convention`
  `dokka-convention`
}

dokka {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }

  dokkatooSourceSets.javaMain {
    displayName = "Java"
  }

  // non-main source sets are suppressed by default
  dokkatooSourceSets.javaMongodbSupport {
    suppress = false
    displayName = "MongoDB"
  }
}
