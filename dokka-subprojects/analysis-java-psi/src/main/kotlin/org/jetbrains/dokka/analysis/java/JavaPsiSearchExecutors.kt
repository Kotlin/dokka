/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java

import com.intellij.codeInsight.JavaExpressionTypeNullabilityPatcher
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.impl.search.MethodSuperSearcher
import com.intellij.psi.search.searches.SuperMethodsSearch
import org.jetbrains.dokka.InternalDokkaApi

/**
 * Registers the extension points and executors of the Java PSI that Dokka needs but the standalone
 * analysis environment (the one built by the Kotlin Analysis API standalone session) does not
 * provide.
 *
 * Must be called once the application environment exists (i.e. after the analysis session has been built)
 * and before any Java documentation is parsed. [disposable] ties the executor registration to that environment.
 *
 * Currently this covers:
 * * `com.intellij.superMethodsSearch` needs an executor: [SuperMethodsSearch] is what
 *   `PsiMethod.findSuperMethods()` delegates to, and since platform 261 its only executor
 *   ([MethodSuperSearcher]) lives in `java-indexing-impl`, which Dokka does not depend on. Without an
 *   executor the search yields nothing and `findSuperMethods()` silently returns an empty array, breaking
 *   Java documentation inheritance.
 * * `com.intellij.java.expressionTypeNullabilityPatcher` needs to be declared: it is consulted
 *   unconditionally by [PsiMethodCallExpressionImpl][com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl]
 *   whenever the type of a method call expression is computed (e.g. while resolving the arguments of an
 *   outer call, or evaluating a field's constant value).
 */
@InternalDokkaApi
public fun registerJavaPsiExtensionPoints(disposable: Disposable) {
    val area = ApplicationManager.getApplication().extensionArea
    if (!area.hasExtensionPoint(JavaExpressionTypeNullabilityPatcher.EP_NAME)) {
        CoreApplicationEnvironment.registerExtensionPoint(
            area,
            JavaExpressionTypeNullabilityPatcher.EP_NAME,
            JavaExpressionTypeNullabilityPatcher::class.java,
        )
    }

    val superMethodsSearch = SuperMethodsSearch.EP_NAME.point
    if (superMethodsSearch.extensionList.none { it is MethodSuperSearcher }) {
        superMethodsSearch.registerExtension(MethodSuperSearcher(), disposable)
    }
}
