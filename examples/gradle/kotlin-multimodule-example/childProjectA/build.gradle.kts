plugins {
    kotlin("jvm")
    `dokka-convention`
}

dokka {
    dokkatooSourceSets.configureEach {
        includes.from("ModuleA.md")
    }
}
