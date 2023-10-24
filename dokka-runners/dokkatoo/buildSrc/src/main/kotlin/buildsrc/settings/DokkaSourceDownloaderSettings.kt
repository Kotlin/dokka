package buildsrc.settings

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property

abstract class DokkaSourceDownloaderSettings : ExtensionAware {

  abstract val dokkaVersion: Property<String>

  companion object {
    const val EXTENSION_NAME = "dokkaSourceDownload"
  }
}
