/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.resolve.source.getPsi

internal class DescriptorDocumentableSource(val descriptor: DeclarationDescriptor) : DocumentableSource {
    override val path = descriptor.toSourceElement.containingFile.toString()

    override fun computeLineNumber(): Int? {
        return (this.descriptor as DeclarationDescriptorWithSource)
            .source.getPsi()
            ?.let {
                val range = it.node?.findChildByType(KtTokens.IDENTIFIER)?.textRange ?: it.textRange
                val doc = PsiDocumentManager.getInstance(it.project).getDocument(it.containingFile)
                // IJ uses 0-based line-numbers; external source browsers use 1-based
                doc?.getLineNumber(range.startOffset)?.plus(1)
            }
    }
}
