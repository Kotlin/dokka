import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.dokka.gradle.DokkaTask

fun Project.dokka(configuration: DokkaTask.() -> Unit) {
    tasks.withType<DokkaTask>().configureEach(configuration)
}
