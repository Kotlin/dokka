plugins {
  `java-application-convention`
  `dokka-convention`
}

dependencies {
  implementation(project(":my-java-library"))
}

dokka {
  dokkatooSourceSets.configureEach {
    includes.from("Module.md")
  }
}

application {
  mainClass = "demo.MyJavaApplication"
}
