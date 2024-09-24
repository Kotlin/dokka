plugins {
    kotlin("jvm") version "1.9.23"
    id("org.jetbrains.dokka-javadoc") version "2.0.20-SNAPSHOT"
}

dokka {
    // used as project name in the header
    moduleName.set("Dokka Gradle Javadoc Example")

    dokkaSourceSets.main {

        // contains descriptions for the module and the packages
        includes.from("Module.md")

        // adds source links that lead to this repository, allowing readers
        // to easily find source code for inspected declarations
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-gradle-javadoc-example/src/main/kotlin")
            remoteLineSuffix.set("#L")
        }
    }
}
