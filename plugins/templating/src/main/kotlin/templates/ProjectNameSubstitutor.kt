package templates

import org.jetbrains.dokka.base.templating.ProjectNameSubstitutionCommand
import org.jetbrains.dokka.base.templating.SubstitutionCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.templates.Substitutor
import org.jetbrains.dokka.templates.TemplatingContext

class ProjectNameSubstitutor(private val dokkaContext: DokkaContext) : Substitutor {

    override fun trySubstitute(context: TemplatingContext<SubstitutionCommand>, match: MatchResult): String? =
        dokkaContext.configuration.moduleName.takeIf { context.command is ProjectNameSubstitutionCommand }
}