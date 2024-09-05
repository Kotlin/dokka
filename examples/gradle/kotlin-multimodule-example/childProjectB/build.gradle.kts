plugins {
    kotlin("jvm")
    `dokka-convention`
}

dokka {
    dokkatooSourceSets.configureEach {
        includes.from("ModuleB.md")
    }
}

//region DON'T COPY - this is only needed for internal Dokkatoo integration tests
dokka {
    modulePath.set("childProjectB") // match the original dokka default
}
tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
    generator.dokkaSourceSets.configureEach {
        sourceSetScope.set(":parentProject:childProjectB:dokkaHtmlPartial")
    }
}
//endregion
