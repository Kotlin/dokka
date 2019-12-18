package  org.jetbrains.dokka.tc.plugin

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.plugability.DokkaPlugin

class TcPlugin : DokkaPlugin() {
  val typeChecking by extending {
    CoreExtensions.descriptorToDocumentationTranslator with TCDescriptorToDocumentationTranslator
  }
}
