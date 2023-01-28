import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation(kotlin("stdlib"))
}

// configuration specific to this subproject.
// notice the use of Partial task
tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        configureEach {
            includes.from("ModuleA.md")
        }
    }
}
