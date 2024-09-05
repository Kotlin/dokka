plugins {
    kotlin("jvm")
    `dokka-convention`
}

//region DON'T COPY - this is only needed for internal Dokkatoo integration tests
dokka {
    modulePath.set("childProjectA") // match the original dokka default
}
tasks.withType<dev.adamko.dokkatoo.tasks.DokkatooGenerateTask>().configureEach {
    generator.dokkaSourceSets.configureEach {
        sourceSetScope.set(":parentProject:childProjectA:dokkaHtmlPartial")
    }
}
//endregion
