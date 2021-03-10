package org.jetbrains.dokka.base.templating

data class AddToSourcesetDependencies(val moduleName: String, val content: Map<String, List<String>>) : Command