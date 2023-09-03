/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.templating

public data class AddToSourcesetDependencies(
    val moduleName: String,
    val content: Map<String, List<String>>
) : Command
