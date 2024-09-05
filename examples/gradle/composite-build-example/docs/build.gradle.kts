plugins {
    id("dokka-convention")
}

dependencies {
    dokka("foo.example:module-kakapo")
    dokka("foo.example:module-kea")
}

dokka {
    moduleName = "Dokka Composite Builds Example"
}
