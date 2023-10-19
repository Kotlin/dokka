package buildsrc.settings

import org.gradle.api.attributes.Attribute
import org.gradle.api.file.Directory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider

/**
 * Settings for the [buildsrc.conventions.Maven_publish_test_gradle] convention plugin.
 */
abstract class MavenPublishTestSettings(
  val testMavenRepo: Provider<Directory>
) : ExtensionAware {
  val testMavenRepoPath: Provider<String> = testMavenRepo.map { it.asFile.invariantSeparatorsPath }

  companion object {
    val attribute = Attribute.of("maven-publish-test", String::class.java)
  }
}
