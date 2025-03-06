/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// absolute reference of declaration or anything else which could be referenced inside module or via external reference
// should contain info about package, class, function, parameters
public typealias KdSymbolId = String

public interface KdSymbol : KdDocumented {
    public val id: KdSymbolId
    public val name: String // display, original name from code
}

// package org.example.lib
// context(_: org.example.lib.Context)
// fun org.example.lib.Type.longName(
//   a: org.example.lib.Parameter1,
//   b: org.example.lib.Parameter2
//   c: org.example.lib.Parameter3
//): org.example.lib.Return

// kind=function,package=org.example.lib,

// in kdoc: [longName(P1, P2, P3)] - should be possible

// TODO: should we be able to reference both overloaded declaration and not?

// symbols:
// - package
// - declaration (class, function, property)
// - article (external documentation file)
// - sample
// -
