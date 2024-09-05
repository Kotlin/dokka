/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
  base
  `dokka-convention`
}

dependencies {
  dokkatoo(project(":my-java-application"))
  dokkatoo(project(":my-java-features"))
  dokkatoo(project(":my-java-library"))

  dokkatooPluginHtml("org.jetbrains.dokka:templating-plugin")
}

dokka {
  moduleName.set("My Java Project")
}
