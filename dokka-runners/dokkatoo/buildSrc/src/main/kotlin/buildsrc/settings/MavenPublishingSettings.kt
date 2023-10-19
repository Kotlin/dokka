package buildsrc.settings

import java.io.File
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.*


/**
 * Settings for the [buildsrc.conventions.Maven_publish_test_gradle] convention plugin.
 */
abstract class MavenPublishingSettings @Inject constructor(
  private val project: Project,
  private val providers: ProviderFactory,
) {

  private val isReleaseVersion: Provider<Boolean> =
    providers.provider { !project.version.toString().endsWith("-SNAPSHOT") }

  val sonatypeReleaseUrl: Provider<String> =
    isReleaseVersion.map { isRelease ->
      if (isRelease) {
        "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
      } else {
        "https://s01.oss.sonatype.org/content/repositories/snapshots/"
      }
    }

  val mavenCentralUsername: Provider<String> =
    d2Prop("mavenCentralUsername")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_USERNAME"))
  val mavenCentralPassword: Provider<String> =
    d2Prop("mavenCentralPassword")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_PASSWORD"))

  val signingKeyId: Provider<String> =
    d2Prop("signing.keyId")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_KEY_ID"))
  val signingKey: Provider<String> =
    d2Prop("signing.key")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_KEY"))
  val signingPassword: Provider<String> =
    d2Prop("signing.password")
      .orElse(providers.environmentVariable("MAVEN_SONATYPE_SIGNING_PASSWORD"))

  val githubPublishDir: Provider<File> =
    providers.environmentVariable("GITHUB_PUBLISH_DIR").map { File(it) }

  private fun d2Prop(name: String): Provider<String> =
    providers.gradleProperty("org.jetbrains.dokka.dokkatoo.$name")

  private fun <T : Any> d2Prop(name: String, convert: (String) -> T): Provider<T> =
    d2Prop(name).map(convert)

  companion object {
    const val EXTENSION_NAME = "mavenPublishing"

    /** Retrieve the [KayrayBuildProperties] extension. */
    internal val Project.mavenPublishing: MavenPublishingSettings
      get() = extensions.getByType()

    /** Configure the [KayrayBuildProperties] extension. */
    internal fun Project.mavenPublishing(configure: MavenPublishingSettings.() -> Unit) =
      extensions.configure(configure)
  }
}
