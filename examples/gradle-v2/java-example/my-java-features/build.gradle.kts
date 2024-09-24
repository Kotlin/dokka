plugins {
    `java-mongodb-library-convention`
    `dokka-convention`
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
    }

    dokkaSourceSets.javaMain {
        displayName = "Java"
    }

    // non-main source sets are suppressed by default
    dokkaSourceSets.javaMongodbSupport {
        suppress = false
        displayName = "MongoDB"
    }
}
