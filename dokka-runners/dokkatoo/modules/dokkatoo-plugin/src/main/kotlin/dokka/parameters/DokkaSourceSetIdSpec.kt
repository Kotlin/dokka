package org.jetbrains.dokka.dokkatoo.dokka.parameters

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import java.io.Serializable
import javax.inject.Inject
import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.*

abstract class DokkaSourceSetIdSpec
@DokkatooInternalApi
@Inject
constructor(
  /**
   * Unique identifier of the scope that this source set is placed in.
   * Each scope provide only unique source set names.
   *
   * TODO update this doc - DokkaTask doesn't represent one source set scope anymore
   *
   * E.g. One DokkaTask inside the Gradle plugin represents one source set scope, since there cannot be multiple
   * source sets with the same name. However, a Gradle project will not be a proper scope, since there can be
   * multiple DokkaTasks that contain source sets with the same name (but different configuration)
   */
  @get:Input
  val scopeId: String,

  @get:Input
  val sourceSetName: String,
) : Named, Serializable {

  @Internal
  override fun getName(): String = "$scopeId/$sourceSetName"

  override fun toString(): String = "DokkaSourceSetIdSpec($scopeId/$sourceSetName)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DokkaSourceSetIdSpec) return false

    if (scopeId != other.scopeId) return false
    return sourceSetName == other.sourceSetName
  }

  override fun hashCode(): Int {
    var result = scopeId.hashCode()
    result = 31 * result + sourceSetName.hashCode()
    return result
  }

  companion object {

    /** Utility for creating a new [DokkaSourceSetIdSpec] instance using [ObjectFactory.newInstance] */
    @DokkatooInternalApi
    fun ObjectFactory.dokkaSourceSetIdSpec(
      scopeId: String,
      sourceSetName: String,
    ): DokkaSourceSetIdSpec = newInstance<DokkaSourceSetIdSpec>(scopeId, sourceSetName)
  }
}
