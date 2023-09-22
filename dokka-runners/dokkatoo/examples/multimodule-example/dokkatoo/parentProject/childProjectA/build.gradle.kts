plugins {
  kotlin("jvm")
  `dokka-convention`
}

dokkatoo {
  dokkatooSourceSets.configureEach {
    includes.from("ModuleA.md")
  }
}

//region DON'T COPY - this is only needed for internal Dokkatoo integration tests
dokkatoo {
  modulePath.set("childProjectA") // match the original dokka default
}
tasks.withType<org.jetbrains.dokka.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
  generator.dokkaSourceSets.configureEach {
    sourceSetScope.set(":parentProject:childProjectA:dokkaHtmlPartial")
  }
}
//endregion
