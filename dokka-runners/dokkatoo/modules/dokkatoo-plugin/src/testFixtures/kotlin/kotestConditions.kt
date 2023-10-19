package org.jetbrains.dokka.dokkatoo.utils

import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.spec.Spec
import kotlin.reflect.KClass

class NotWindowsCondition : EnabledCondition {
  override fun enabled(kclass: KClass<out Spec>): Boolean =
    "win" !in System.getProperty("os.name").lowercase()
}
