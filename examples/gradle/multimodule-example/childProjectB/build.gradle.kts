plugins {
    kotlin("jvm")
    `dokka-convention`
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("ModuleB.md")
    }
}
