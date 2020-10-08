package org.jetbrains.dokka.base.translators.documentables

import org.jetbrains.dokka.model.DFunction

interface DocumentableFilteringStrategies {
    fun shouldConstructorBeInPage(constructor: DFunction): Boolean
}
