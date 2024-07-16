package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.DokkatooBasePlugin
import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask

/** Base Dokkatoo task */
@CacheableTask
abstract class DokkatooTask
@DokkatooInternalApi
constructor() : DefaultTask() {

  @get:Inject
  abstract val objects: ObjectFactory

  init {
    group = DokkatooBasePlugin.TASK_GROUP
  }
}
