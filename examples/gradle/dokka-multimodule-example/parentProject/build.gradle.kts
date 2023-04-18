import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import java.net.URL

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

            // In multi-project builds, `remoteUrl` must point to that project's dir specifically, so if you
            // want to configure sourceLinks at once in `subprojects {}`, you have to find the relative path.
            // Alternatively, you can move this configuration up into subproject build scripts,
            // and just hardcode the exact paths as demonstrated in the basic dokka-gradle-example.
            sourceLink {
                val exampleDir = "https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-multimodule-example"
                val projectRelativePath = rootProject.projectDir.toPath().relativize(projectDir.toPath())

                localDirectory.set(file("src"))
                remoteUrl.set(URL("$exampleDir/$projectRelativePath/src"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}

// Configures only the parent MultiModule task,
// this will not affect subprojects
tasks.dokkaHtmlMultiModule {
    moduleName.set("Dokka MultiModule Example")
}
