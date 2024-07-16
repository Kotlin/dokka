package dev.adamko.dokkatoo.dokka.parameters

import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input

internal interface HasConfigurableVisibilityModifiers {

  @get:Input
  val documentedVisibilities: SetProperty<VisibilityModifier>

  /** Sets [documentedVisibilities] (overrides any previously set values). */
  fun documentedVisibilities(vararg visibilities: VisibilityModifier): Unit =
    documentedVisibilities.set(visibilities.asList())
}
