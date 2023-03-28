package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.java

import com.intellij.psi.PsiNamedElement
import org.jetbrains.dokka.analysis.java.DocComment
import org.jetbrains.dokka.analysis.java.DocCommentCreator
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.KDocFinder
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.DescriptorFinder
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement

internal class KotlinDocCommentCreator(
    private val kdocFinder: KDocFinder,
    private val descriptorFinder: DescriptorFinder
) : DocCommentCreator {
    override fun create(element: PsiNamedElement): DocComment? {
        val ktElement = element.navigationElement as? KtElement ?: return null
        val kdoc = with (kdocFinder) {
            ktElement.findKDoc()
        } ?: return null
        val descriptor = with (descriptorFinder) {
            (element.navigationElement as? KtDeclaration)?.findDescriptor()
        } ?: return null

        return KotlinDocComment(kdoc, descriptor)
    }
}
