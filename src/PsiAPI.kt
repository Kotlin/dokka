package com.jetbrains.dokka

import com.intellij.psi.PsiElement
import kotlin.support.AbstractIterator

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
