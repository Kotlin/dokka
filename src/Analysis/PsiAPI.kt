package org.jetbrains.dokka

import com.intellij.psi.*
import kotlin.support.*

fun PsiElement.children(): Stream<PsiElement> {
    val parent = this
    var current: PsiElement? = null
    return object : Stream<PsiElement> {
        override fun iterator(): Iterator<PsiElement> = object : AbstractIterator<PsiElement>() {
            {
                setNext(parent.getFirstChild())
            }
            override fun computeNext() {
                current = current?.getNextSibling()
                if (current == null)
                    done()
                else
                    setNext(current!!)
            }
        }
    }
}
