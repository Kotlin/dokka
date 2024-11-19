plugins {
    `java-library-convention`
    `dokka-convention`
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
    }
}
