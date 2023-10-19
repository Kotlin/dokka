package buildsrc.conventions

plugins {
  id("buildsrc.conventions.base")
  id("buildsrc.conventions.java-base")
  id("org.gradle.kotlin.kotlin-dsl")
  id("com.gradle.plugin-publish")
}

tasks.validatePlugins {
  enableStricterValidation.set(true)
}

val createJavadocJarReadme by tasks.registering(Sync::class) {
  description = "generate a readme.txt for the Javadoc JAR"
  from(
    resources.text.fromString(
      """
      This Javadoc JAR is intentionally empty.
      
      For documentation, see the sources JAR or https://github.com/adamko-dev/dokkatoo/
      
    """.trimIndent()
    )
  ) {
    rename { "readme.txt" }
  }
  into(temporaryDir)
}


// The Gradle Publish Plugin enables the Javadoc JAR in afterEvaluate, so find it lazily
tasks.withType<Jar>()
  .matching { it.name == "javadocJar" }
  .configureEach {
    from(createJavadocJarReadme)
  }
