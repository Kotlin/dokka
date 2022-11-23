import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    id("com.github.johnrengelman.shadow")
}

val dokka_version: String by project
evaluationDependsOn(":runners:cli")
evaluationDependsOn(":plugins:base")
evaluationDependsOn(":plugins:html")

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("test-junit"))
}

/* Create a fat base plugin jar for cli tests */
val basePluginShadow: Configuration by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
    }
}

val htmlPluginShadow: Configuration by configurations.creating {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "java-runtime"))
    }
}

dependencies {
    basePluginShadow(project(":plugins:base"))
    basePluginShadow(project(":kotlin-analysis")) // compileOnly in base plugin
    htmlPluginShadow(project(":plugins:html"))
}

val basePluginShadowJar by tasks.register("basePluginShadowJar", ShadowJar::class) {
    configurations = listOf(basePluginShadow)
    archiveFileName.set("fat-base-plugin-$dokka_version.jar")
    archiveClassifier.set("")
}
val htmlPluginShadowJar by tasks.register("htmlPluginShadowJar", ShadowJar::class) {
    configurations = listOf(htmlPluginShadow)
    archiveFileName.set("fat-html-plugin-$dokka_version.jar")
    archiveClassifier.set("")
}

tasks.integrationTest {
    inputs.dir(file("projects"))
    val cliJar = tasks.getByPath(":runners:cli:shadowJar") as ShadowJar
    environment("CLI_JAR_PATH", cliJar.archiveFile.get())
    environment("HTML_PLUGIN_JAR_PATH", htmlPluginShadowJar.archiveFile.get())
    environment("BASE_PLUGIN_JAR_PATH", basePluginShadowJar.archiveFile.get())
    dependsOn(cliJar)
    dependsOn(basePluginShadowJar)
    dependsOn(htmlPluginShadowJar)
}

