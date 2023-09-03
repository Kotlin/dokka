/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.markdown.jb

import org.intellij.markdown.MarkdownElementTypes
import org.jetbrains.dokka.InternalDokkaApi

// TODO [beresnev] move/rename if it's only used for CustomDocTag. for now left as is for compatibility
@InternalDokkaApi
public val MARKDOWN_ELEMENT_FILE_NAME: String = MarkdownElementTypes.MARKDOWN_FILE.name
