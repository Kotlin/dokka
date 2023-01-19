import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

// You can apply and configure Dokka in each subproject
// individially or configure all subprojects at once
subprojects {
    apply(plugin = "org.jetbrains.dokka")

    tasks.withType<DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            documentedVisibilities.set(setOf(
                Visibility.PUBLIC,
                Visibility.PROTECTED
            ))
        }
    }
}

// Configures only the parent MultiModule task,
// this will not affect subprojects
tasks.dokkaHtmlMultiModule {
    moduleName.set("Dokka MultiModule Example")
}

dependencies {
    implementation(kotlin("stdlib"))
}

