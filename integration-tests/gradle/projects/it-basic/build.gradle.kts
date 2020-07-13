plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

apply(from = "../template.root.gradle.kts")

dependencies {
    implementation(kotlin("stdlib"))
}

tasks.dokkaHtml {
    dokkaSourceSets {
        register("global") {
            jdkVersion = 2
        }
    }
}
