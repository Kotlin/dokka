plugins {
    kotlin("jvm")
    `dokka-convention`
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("ModuleA.md")
    }
}
