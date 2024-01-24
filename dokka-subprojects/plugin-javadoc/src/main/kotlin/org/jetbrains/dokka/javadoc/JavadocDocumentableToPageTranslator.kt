/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.javadoc

import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.pages.RootPageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.documentation.DocumentableToPageTranslator

public class JavadocDocumentableToPageTranslator(
    private val context: DokkaContext
) : DocumentableToPageTranslator {
    override fun invoke(module: DModule): RootPageNode = JavadocPageCreator(context).pageForModule(module)
}
