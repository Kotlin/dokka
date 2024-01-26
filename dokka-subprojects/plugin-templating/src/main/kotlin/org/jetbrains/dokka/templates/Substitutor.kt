/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.templates

import org.jetbrains.dokka.base.templating.SubstitutionCommand

public fun interface Substitutor {
    public fun trySubstitute(context: TemplatingContext<SubstitutionCommand>, match: MatchResult): String?
}
