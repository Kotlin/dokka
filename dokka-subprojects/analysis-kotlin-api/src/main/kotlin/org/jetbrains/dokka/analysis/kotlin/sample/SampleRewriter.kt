/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.sample

/**
 * Represents a function name
 */
public typealias ShortFunctionName = String

/**
 * Rewrites an expression of function call to an arbitrary string
 *
 * @see rewrite
 */
public interface FunctionCallRewriter{
    /**
     *
     * A value argument and type arguments represents a string from source code in [argumentList] and [typeArgumentList]
     * E.g., `f(1, "s")` will be passed to [rewrite] as `"1"` and `"\"s\""`
     *
     * In the case of a [trailing lambda](https://kotlinlang.org/docs/lambdas.html#passing-trailing-lambdas),
     * it will be passed as it in the last element of [argumentList]
     * E.g., the snippet `f(0) { i++ }` will have `"0"` and `"{ i++ }"` arguments
     *
     * @param argumentList a list of arguments represented as a string from source code
     * @param typeArgumentList a list of type parameters represented as a string from source code
     *
     * @return a string or `null` to delete the call expression
     */
    public fun rewrite(argumentList: List<String>, typeArgumentList: List<String>): String?
}

/**
 * Rewrites Kotlin sample snippet
 *
 * For simplicity's sake, it processes sample code syntactically (AST) and
 * does not have the information about semantics of code, e.g. resolved types.
 * So it works only with strings without addition abstractions.
 *
 * @see SampleSnippet
 */
public interface SampleRewriter {
    /**
     * Allows to rewrite paths of [import directives](https://kotlinlang.org/spec/packages-and-imports.html#importing)
     * E.g., for `import org.example` the path `org.example` will be used as an argument of [rewriteImportDirective]
     *
     * The default implementation is the identity function that return unchanged an import path
     *
     * @param importPath a path of value of import directive
     *
     * @return a string or `null` to delete a whole import directive in [SampleSnippet.imports]
     *
     * @see SampleSnippet.imports
     */
    public fun rewriteImportDirective(importPath: String): String? = importPath

    /**
     * Allows to rewrite a function call expressions
     * It also includes calls of constructors
     *
     * The remain call expressions of functions that are not in this map will be left unchanged
     */
    public val functionCallRewriters: Map<ShortFunctionName, FunctionCallRewriter>
}

