package org.jetbrains.dokka.base.templating

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS

@JsonTypeInfo(use = CLASS)
interface Command

abstract class SubstitutionCommand : Command {
    abstract val pattern: String
}
