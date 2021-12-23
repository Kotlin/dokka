plugins {
    id("org.jetbrains.dokka")
    kotlin("js")
}

apply(from = "../template.root.gradle.kts")

kotlin {
    js(IR) {
        browser()
        nodejs()
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(npm("is-sorted", "1.0.5"))
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom:${properties["dokka_it_react_kotlin_version"]}")
}