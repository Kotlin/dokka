package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.HasFormatName

@DokkatooInternalApi
class TaskNames(override val formatName: String) : HasFormatName() {
  val generate = "dokkatooGenerate".appendFormat()
  val generatePublication = "dokkatooGeneratePublication".appendFormat()
  val generateModule = "dokkatooGenerateModule".appendFormat()
  val consolidateModuleElements = "dokkatooConsolidateModuleElements".appendFormat()
  val prepareModuleDescriptor = "prepareDokkatooModuleDescriptor".appendFormat()
}
