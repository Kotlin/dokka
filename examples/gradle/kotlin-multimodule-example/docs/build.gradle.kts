plugins {
    kotlin("jvm") apply false
    `dokka-convention`
}

dependencies {
    dokkatoo(project(":parentProject:childProjectA"))
    dokkatoo(project(":parentProject:childProjectB"))
}

dokka {
    moduleName.set("Dokka MultiModule Example")

    dokkaPublications.html {
        includes.from("allmodules.md")
    }
}
