import com.github.gradle.node.npm.task.NpmTask
import org.jetbrains.kotlin.util.parseSpaceSeparatedArgs

@Suppress("DSL_SCOPE_VIOLATION") // fixed in Gradle 8.1 https://github.com/gradle/gradle/pull/23639
plugins {
    id("org.jetbrains.conventions.dokka-base-frontend-files")
    alias(libs.plugins.gradleNode)
}

node {
    version.set(libs.versions.node)

    // https://github.com/node-gradle/gradle-node-plugin/blob/3.5.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
    download.set(true)
    distBaseUrl.set(null as String?) // Strange cast to avoid overload ambiguity
}

val distributionDirectory = layout.projectDirectory.dir("dist")

val npmRunBuild by tasks.registering(NpmTask::class) {
    dependsOn(tasks.npmInstall)

    npmCommand.set(parseSpaceSeparatedArgs("run build"))

    inputs.dir("src/main")
    inputs.files(
        "package.json",
        "webpack.config.js",
    )

    outputs.dir(distributionDirectory)
    outputs.cacheIf { true }
}

configurations.dokkaBaseFrontendFilesElements.configure {
    outgoing {
        artifact(distributionDirectory) {
            builtBy(npmRunBuild)
        }
    }
}

tasks.clean {
    delete(
        file("node_modules"),
        file("dist"),
    )
}
