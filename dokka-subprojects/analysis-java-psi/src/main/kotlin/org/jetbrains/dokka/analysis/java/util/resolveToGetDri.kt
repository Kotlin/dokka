/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.util.InheritanceUtil

// TODO [beresnev] get rid of
internal fun PsiElement.resolveToGetDri(): PsiElement? =
    reference?.resolveToDocumentedElement()

/**
 * Resolves a JavaDoc reference (used for `{@link}`, `{@linkplain}` and `@see` tags).
 *
 * [PsiReference.resolve] returns `null` for an ambiguous method reference. This happens, in particular,
 * when a method overrides a method from the classpath that is also declared in several inherited
 * interfaces: e.g. `LinkedList#getFirst` is also declared in `Deque` and `SequencedCollection`, so a
 * `{@link #getFirst()}` inside a `LinkedList` subclass resolves to more than one candidate.
 *
 * In the IDE such a reference offers a choice between navigating to the parent or to the child
 * declaration. Here we default to the child (most-derived) declaration, since the parent can always be
 * linked explicitly via its class name.
 *
 * See https://github.com/Kotlin/dokka/issues/4543
 */
internal fun PsiReference.resolveToDocumentedElement(): PsiElement? {
    resolve()?.let { return it }
    if (this is PsiPolyVariantReference) {
        return multiResolve(false).mapNotNull { it.element }.mostDerivedMethodOrNull()
    }
    return null
}

/**
 * Returns the single method whose containing class is a subtype of every other candidate's containing
 * class, or `null` if there is no such method (i.e. the candidates are genuinely ambiguous or not all
 * methods). This is used to pick the overriding (child) declaration among the candidates of a JavaDoc
 * method reference.
 */
private fun List<PsiElement>.mostDerivedMethodOrNull(): PsiElement? {
    val methods = filterIsInstance<PsiMethod>()
    if (methods.isEmpty() || methods.size != size) return null
    return methods.singleOrNull { candidate ->
        val candidateClass = candidate.containingClass ?: return@singleOrNull false
        methods.all { other ->
            other === candidate || other.containingClass?.let { otherClass ->
                InheritanceUtil.isInheritorOrSelf(candidateClass, otherClass, true)
            } == true
        }
    }
}
