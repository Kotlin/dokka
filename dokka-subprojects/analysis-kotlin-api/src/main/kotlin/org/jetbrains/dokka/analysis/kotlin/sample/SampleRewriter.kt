/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.sample

public typealias ShortFunctionName = String

public interface FunctionCallRewriter{
    /**
     * In the case of a [trailing lambda](https://kotlinlang.org/docs/lambdas.html#passing-trailing-lambdas),
     * it will be passed as it in the last element of [argumentList]
     * E.g., the snippet `f(0) { i++ }` will have `"0"` and `"{ i++ }"` arguments
     */
    public fun rewrite(argumentList: List<String>, typeArgumentList: List<String>): String?
}

/**
 * For simplicity's sake, it process sample code syntactically and does not have the information about semantics of code, e.g. resolved types.
 *
 *
 * TODO write KDoc
 */
public interface SampleRewriter {
    /**
     * `null` to delete a whole import directive
     */
    public fun rewriteImportDirective(importPath: String): String? = importPath
    public val functionCallRewriters: Map<ShortFunctionName, FunctionCallRewriter>
}

