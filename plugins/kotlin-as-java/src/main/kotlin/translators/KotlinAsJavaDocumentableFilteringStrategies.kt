package org.jetbrains.dokka.kotlinAsJava.translators

import org.jetbrains.dokka.base.translators.documentables.DocumentableFilteringStrategies
import org.jetbrains.dokka.model.DFunction

object KotlinAsJavaDocumentableFilteringStrategies : DocumentableFilteringStrategies {
    override fun shouldConstructorBeInPage(constructor: DFunction) = true
}
