import dev.adamko.dokkatoo.dokka.plugins.DokkaHtmlPluginParameters

plugins {
  buildsrc.conventions.base
  dev.adamko.`dokkatoo-html`
}

dependencies {
  dokkatoo(projects.modules.dokkatooPlugin)
  dokkatooPluginHtml(libs.kotlin.dokkaPlugin.allModulesPage)
  dokkatooPluginHtml(libs.kotlin.dokkaPlugin.templating)
}

dokkatoo {
  moduleName.set("Dokkatoo Gradle Plugin")

  pluginsConfiguration.named<DokkaHtmlPluginParameters>("html") {
    customStyleSheets.from(
      "./style/logo-styles.css",
    )
    customAssets.from(
      "./images/logo-icon.svg",
    )
  }
}

tasks.dokkatooGeneratePublicationHtml {
  doLast {
    outputDirectory.get().asFile.walk()
      .filter { it.isFile && it.extension == "html" }
      .forEach { file ->
        file.writeText(
          file.readText()
            .replace(
              """<html>""",
              """<html lang="en">""",
            )
            .replace(
              """
                <button id="theme-toggle-button">
              """.trimIndent(),
              """
                <div id="github-link"><a href="https://github.com/adamko-dev/dokkatoo/"></a></div>
                <button id="theme-toggle-button">
              """.trimIndent(),
            )
            .replace(
              """
                href="https://github.com/Kotlin/dokka"><span>dokka</span>
              """.trimIndent(),
              """
                href="https://github.com/adamko-dev/dokkatoo/"><span>Dokkatoo</span>
              """.trimIndent(),
            )
        )
      }
  }
}
