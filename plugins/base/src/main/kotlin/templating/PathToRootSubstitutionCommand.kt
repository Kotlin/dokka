package org.jetbrains.dokka.base.templating

data class PathToRootSubstitutionCommand(override val pattern: String, val default: String): SubstitutionCommand()