package org.jetbrains.dokka.allModulesPage.templates

import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.templating.PathToRootSubstitutionCommand
import org.jetbrains.dokka.base.templating.SubstitutionCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.query
import java.nio.file.Path

class PathToRootSubstitutor(private val dokkaContext: DokkaContext) : Substitutor {
    override fun trySubstitute(context: TemplatingContext<SubstitutionCommand>, match: MatchResult): String? =
        if (context.command is PathToRootSubstitutionCommand) {
            context.output.toPath().parent.relativize(dokkaContext.configuration.outputDir.toPath()).toString() + "/"
        } else null

}