/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

private fun test() {
    KdFragment(
        name = "common",
        elements = listOf(
            KdModule(
                id = KdModuleId("example"),
                name = "example",
                documentation = listOf(KdDocumentationNode.Text("Module docs")),
                packages = listOf(
                    KdPackageId("org.example.test"),
                )
            ),
            KdPackage(
                id = KdPackageId("org.example.test"),
                name = "org.example.test",
                documentation = listOf(KdDocumentationNode.Text("Package docs")),
                classlikes = listOf(
                    KdClassLikeId("org.example.test", "TestClass")
                )
            ),
            KdClass(
                id = KdClassLikeId("org.example.test", "TestClass"),
                name = "TestClass",
                classKind = KdClassKind.CLASS,
                documentation = listOf(KdDocumentationNode.Text("Class docs")),
                callables = listOf(
                    KdCallableId("org.example.test", "TestClass", "test", "0")
                )
            ),
            KdVariable(
                id = KdCallableId("org.example.test", "TestClass", "test", "0"),
                name = "test",
                variableKind = KdVariableKind.PROPERTY,
                returns = KdReturns(KdClassLikeType(KdClassLikeId("kotlin", "String"))),
                documentation = listOf(KdDocumentationNode.Text("Property docs"))
            )
        )
    )
}
