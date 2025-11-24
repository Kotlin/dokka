/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

/*
Workflow:
1. use sources to generate JSON for all fragments (N*KdFragment)
2. take package docs and module docs (topics) from `/docs` (KdModule)
3. get external dependencies links (M*KdExternalModule) (klibs.io + custom)
4. get all modules data - dependencies, other projects, etc (K*KdModule)
5. get project topics (KdProject)
6. generate HTML (input = KdProject + N*KdModule + M*KdExternalModule)
 */

// outputs/inputs:
// - model
// - external links model - for cross-links - can it be used for source links?
// - sources model - for links to sources - sources.jar?
// - how to link to GH sources?


// TODO: external links and source information

// declaration <- fragment <- module <- project

// TODO: we should generate package-list (or module-list) for interoperability with javadoc

/**
 * Overall we do have four parts of what is needed for documentation:
 * - declarations KDocs
 * - docs - module-docs, project-docs, package-docs, additional topics
 * - external links
 * - source links
 */

// during distribution per artifact/module to MC:
// - N KdFragments per source-set
// - 1 KdModule
// - 1 umanifest to describe fragment dependencies


// docs for:
// - project ? (not kotlin-documentation) - not present in code
// - module (a.k.a. package in klibs.io)  - not present in code
// - package                              - not present in code

// - class, function, property, etc

// also we have:
// - samples
// - fragments (a.k.a. source set in KGP) - not present in code
// - external links
// - topics/articles - not present in code
// - expect-actual

// so we have two models to cover:
// - declarations per fragment - coming from code
// - module, package, project, topics - coming from outside, but should support KDoc features (like samples?) and links

// External and internal links should be possible to:
// - packages [ + section]
// - declarations [ + section]
// - topic [ + section]
// - sample - TODO if we need to link to sample outside of `@sample` tag
// not possible and may be not needed below:
// - module - ??? - there is no stable spec for kotlin module
// - project - ??? - there is no stable spec for kotlin project (a.k.a. multiple modules)


// TODO: ids and fragments
// function:org.example//string/abc - common
// function:org.example//string/abc - jvm
// function:org.example//string/abc - native
// function:org.example//notString/abc - jvm
// function:org.example//notString/abc - native

// in commonMain
// expect fun string()
// in jvmMain = (jvm)
// actual fun string()
// fun notString()
// in nativeMain = (ios+macos+linux+windows+...)
// actual fun string()
// fun notString()

// TODO?
// absolute reference of declaration or anything else which could be referenced inside module or via external reference
// should contain info about package, class, function, parameters


// links to declarations/symbols - or KdDeclarationId?
//
//  package:PACKAGE_NAME
//    class:PACKAGE_NAME/CLASS_NAME
// constructor:PACKAGE_NAME/          /PROPERTY_NAME/HASH
// property:PACKAGE_NAME/          /PROPERTY_NAME/HASH
// function:PACKAGE_NAME/          /FUNCTION_NAME/HASH
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME/HASH
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME#HASH

// variable:PACKAGE_NAME/CLASS_NAME/PROPERTY_NAME/0
// variable:PACKAGE_NAME/CLASS_NAME/PROPERTY_NAME/1
// variable:PACKAGE_NAME/CLASS_NAME/ENUM_ENTRY_NAME/0
// function:PACKAGE_NAME/CLASS_NAME//0
// function:PACKAGE_NAME/CLASS_NAME//1
// function:PACKAGE_NAME/CLASS_NAME//2
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME/0
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME/1
// function:PACKAGE_NAME/CLASS_NAME/ENUM_ENTRY_NAME/XXX

// topic:...

private fun test() {
    KdFragment(
        name = "common",
        documentation = listOf(KdDocumentationNode.Text("Module docs")),
        packages = listOf(
            KdPackage(
                name = "org.example.test",
                documentation = listOf(KdDocumentationNode.Text("Package docs")),
                declarations = listOf(
                    KdClass(
                        name = "TestClass",
                        classKind = KdClassKind.CLASS,
                        documentation = listOf(KdDocumentationNode.Text("Class docs")),
                        declarations = listOf(
                            KdVariable(
                                name = "test",
                                variableKind = KdVariableKind.PROPERTY,
                                returns = KdReturns(KdClassifierType(KdClassifierId("kotlin", "String"))),
                                documentation = listOf(KdDocumentationNode.Text("Property docs"))
                            )
                        )
                    )
                )
            )
        )
    )
}

// class : org.example/ClassA - id
// sources.jar: org/example | ClassA.kt - filepath (+ line number)
// github: github.com/whyoleg/example | module-name/src/commonMain/kotlin/org/example | ClassA.kt - URL (+ line number)
// external: klibs.io/whyoleg/example/org.example/ClassA

// module(XXX) -> fragment(commonMain) -> package(org.example) -> file(Hell.kt) -> class -> declaration
