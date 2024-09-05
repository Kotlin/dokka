plugins {
    kotlin("jvm") version "1.9.23"
    id("dev.adamko.dokkatoo") version "2.4.0-SNAPSHOT"
}

dokka {
    // used as project name in the header
    moduleName.set("Dokka Gradle Example")

    dokkatooSourceSets.main {

        // contains descriptions for the module and the packages
        includes.from("Module.md")

        // adds source links that lead to this repository, allowing readers
        // to easily find source code for inspected declarations
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-gradle-example/src/main/kotlin")
            remoteLineSuffix.set("#L")
        }
    }
}
