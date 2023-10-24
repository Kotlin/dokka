package  org.jetbrains.dokka.dokkatoo.utils


fun GradleProjectTest.copyExampleProject(path: String) {
  GradleProjectTest.exampleProjectsDir
    .resolve(path)
    .toFile()
    .copyRecursively(projectDir.toFile(), overwrite = true) { _, _ -> OnErrorAction.SKIP }
}


fun GradleProjectTest.copyIntegrationTestProject(path: String) {
  GradleProjectTest.integrationTestProjectsDir
    .resolve(path)
    .toFile()
    .copyRecursively(projectDir.toFile(), overwrite = true) { _, _ -> OnErrorAction.SKIP }
}
