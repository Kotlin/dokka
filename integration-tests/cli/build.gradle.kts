import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.jetbrains.conventions.dokka-integration-test")
    id("com.github.johnrengelman.shadow")
}

val dokka_version: String by project
evaluationDependsOn(":runners:cli")
evaluationDependsOn(":plugins:base")

dependencies {
    implementation(kotlin("test-junit"))
    implementation(projects.integrationTests)
}

/* Create a fat base plugin jar for cli tests */
val basePluginShadow: Configuration by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
    }
}

dependencies {
    basePluginShadow(projects.plugins.base)
    basePluginShadow(projects.kotlinAnalysis) // compileOnly in base plugin
}

val basePluginShadowJar by tasks.register("basePluginShadowJar", ShadowJar::class) {
    configurations = listOf(basePluginShadow)
    archiveFileName.set("fat-base-plugin-$dokka_version.jar")
    archiveClassifier.set("")
}

tasks.integrationTest {
    inputs.dir(file("projects"))
    val cliJar = tasks.getByPath(":runners:cli:shadowJar") as ShadowJar
    environment("CLI_JAR_PATH", cliJar.archiveFile.get())
    environment("BASE_PLUGIN_JAR_PATH", basePluginShadowJar.archiveFile.get())
    dependsOn(cliJar)
    dependsOn(basePluginShadowJar)
}
