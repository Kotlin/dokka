package org.jetbrains.dokka.templates

import org.jetbrains.dokka.base.templating.PathToRootSubstitutionCommand
import org.jetbrains.dokka.base.templating.SubstitutionCommand
import org.jetbrains.dokka.plugability.DokkaContext
import java.io.File

class PathToRootSubstitutor(private val dokkaContext: DokkaContext) : Substitutor {

    override fun trySubstitute(context: TemplatingContext<SubstitutionCommand>, match: MatchResult): String? =
        if (context.command is PathToRootSubstitutionCommand) {
            context.output.toPath().parent.relativize(dokkaContext.configuration.outputDir.toPath()).toString().split(File.separator).joinToString(separator = "/", postfix = "/") { it }
        } else null
}