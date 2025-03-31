/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// module-based-symbols (unique to module):
// - package
// - article/topic
// - "fragment"

// fragment-based-symbols:
// - property/function/class
//
// - sample - separate fragment

//  package:PACKAGE_NAME
//    class:PACKAGE_NAME/CLASS_NAME
// property:PACKAGE_NAME/          /PROPERTY_NAME
// property:PACKAGE_NAME/          /PROPERTY_NAME/HASH
// function:PACKAGE_NAME/          /FUNCTION_NAME
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME/HASH
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME(PARAM_TYPE_1,PARAM_TYPE_2)
// function:PACKAGE_NAME/CLASS_NAME/(CONTEXT_TYPE_1,CONTEXT_TYPE_2)FUNCTION_NAME(PARAM_TYPE_1,PARAM_TYPE_2)
// function:PACKAGE_NAME/CLASS_NAME/(CONTEXT_TYPE_1,CONTEXT_TYPE_2)RECEIVER_NAME.FUNCTION_NAME(PARAM_TYPE_1,PARAM_TYPE_2)
// function:PACKAGE_NAME/CLASS_NAME/<T:TYPE&TYPE&TYPE,X:TYPE>(CONTEXT_TYPE_1,CONTEXT_TYPE_2)RECEIVER_NAME.FUNCTION_NAME(PARAM_TYPE_1,PARAM_TYPE_2)
// sample:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME
// article:...
// topic:...
// enumentry?
// function:PACKAGE_NAME/CLASS_NAME/FUNCTION_NAME#HASH


// absolute reference of declaration or anything else which could be referenced inside module or via external reference
// should contain info about package, class, function, parameters
public typealias KdDeclarationId = String

// external URL
public typealias KdSymbolLink = String

public interface KdDeclaration : KdNamed, KdDocumented {
    public val id: KdDeclarationId
    public val visibility: KdVisibility
    public val modality: KdModality
    public val annotations: List<KdAnnotation>

    public val isActual: Boolean
    public val isExpect: Boolean
}

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
