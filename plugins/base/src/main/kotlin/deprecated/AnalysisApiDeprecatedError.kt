/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.deprecated

import org.jetbrains.dokka.InternalDokkaApi

// TODO all API that mentions this message or error can be removed in Dokka >= 2.1

internal const val ANALYSIS_API_DEPRECATION_MESSAGE =
    "Dokka's Analysis API has been reworked. Please, see the following issue for details and migration options: " +
            "https://github.com/Kotlin/dokka/issues/3099"

@InternalDokkaApi
public class AnalysisApiDeprecatedError : Error(ANALYSIS_API_DEPRECATION_MESSAGE)
