package org.jetbrains.dokka.base.templating

data class ProjectNameSubstitutionCommand(override val pattern: String, val default: String): SubstitutionCommand()