/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package templates

import org.jetbrains.dokka.base.templating.ProjectNameSubstitutionCommand
import org.jetbrains.dokka.base.templating.SubstitutionCommand
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.templates.Substitutor
import org.jetbrains.dokka.templates.TemplatingContext

public class ProjectNameSubstitutor(
    private val dokkaContext: DokkaContext
) : Substitutor {

    override fun trySubstitute(context: TemplatingContext<SubstitutionCommand>, match: MatchResult): String? =
        dokkaContext.configuration.moduleName.takeIf { context.command is ProjectNameSubstitutionCommand }
}
