plugins {
    kotlin("jvm") apply false
    `dokka-convention`
}

dependencies {
    dokka(project(":childProjectA"))
    dokka(project(":childProjectB"))
}

dokka {
    moduleName.set("Dokka MultiModule Example")

    dokkaPublications.html {
        includes.from("DocsModule.md")
    }
}
