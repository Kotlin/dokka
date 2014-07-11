package org.jetbrains.dokka

import com.intellij.psi.*
import kotlin.support.*

fun PsiElement.previousSiblings(): Stream<PsiElement> {
    var element: PsiElement? = this
    return object : Stream<PsiElement> {
        override fun iterator(): Iterator<PsiElement> = object : AbstractIterator<PsiElement>() {
            override fun computeNext() {
                element = element?.getPrevSibling()
                if (element == null)
                    done()
                else
                    setNext(element!!)
            }
        }
    }
}
