plugins {
    kotlin("jvm") apply false
    `dokka-convention`
}

dependencies {
    dokka(project(":parentProject:childProjectA"))
    dokka(project(":parentProject:childProjectB"))
}

dokka {
    moduleName.set("Dokka MultiModule Example")

    dokkaPublications.html {
        includes.from("allmodules.md")
    }
}
