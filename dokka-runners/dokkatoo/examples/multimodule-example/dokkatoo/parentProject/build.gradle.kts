plugins {
  kotlin("jvm") apply false
  `dokka-convention`
}

dependencies {
  dokkatoo(project(":parentProject:childProjectA"))
  dokkatoo(project(":parentProject:childProjectB"))
  dokkatooPluginHtml(
    dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
      "org.jetbrains.dokka:all-modules-page-plugin:$dokkaVersion"
    }
  )
  dokkatooPluginHtml(
    dokkatoo.versions.jetbrainsDokka.map { dokkaVersion ->
      "org.jetbrains.dokka:templating-plugin:$dokkaVersion"
    }
  )
}

dokkatoo {
  moduleName.set("Dokka MultiModule Example")
}
