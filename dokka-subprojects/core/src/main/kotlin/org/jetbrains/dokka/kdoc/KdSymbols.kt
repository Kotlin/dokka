/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

/*
common:
  expect class Foo {
    fun a()
  }
nonJvm:
  expect class Foo {
    fun a()
    fun b()
  }
native:
  actual class Foo {
    fun a() // from common
    fun b() // from nonJvm (same as in jvm)
    fun c() // from HERE
  }
jvm:
  actual class Foo {
    fun a() // from common
    fun b() // from HERE (same as in nonJvm)
  }
 */

public interface KdPackageSymbol : KdSymbol

// declaration has both package and fragment
public interface KdDeclarationSymbol : KdSymbol {
    public val visibility: KdVisibility
    public val annotations: List<KdAnnotation>
}

public interface KdClassLikeSymbol : KdDeclarationSymbol

// interface, object, class, annotation, enum
public interface KdClassSymbol : KdClassLikeSymbol {
//    public val classKind = interface, class, ...

    // all other fields here...
    public val isCompanion: Boolean
    public val isData: Boolean
    public val isValue: Boolean
    public val isInner: Boolean

    // relation
//    public val superTypes: List<KdType>

    // relations
    // TODO: decide on how to represent functions, properties, nested classes, objects, static fields (java)
//    public val declarations: List<KdSymbolReference>
//    public val members: List<KdSymbolReference>
//    public val classlikes: List<KdSymbolReference>
//    public val callables: List<KdSymbolReference>

    // generics?

    public val samples: List<KdSymbolId>
    public val tags: List<KdTag>
}

public interface KdEnumSymbol : KdClassSymbol {
    public val entries: List<KdEnumEntrySymbol>
}

public interface KdEnumEntrySymbol : KdDeclarationSymbol

public interface KdTypeAliasSymbol : KdClassLikeSymbol {
    public val typeParameters: List<KdTypeParameter>
    public val underlyingType: KdType
}

public interface KdCallableSymbol : KdDeclarationSymbol {
    public val returnType: KdType

    public val receiverParameter: KdReceiverParameter?
    public val contextParameters: List<KdContextParameter>
}

public interface KdFunctionLikeSymbol : KdCallableSymbol {
    public val valueParameters: List<KdValueParameter>
}

public interface KdConstructorSymbol : KdFunctionLikeSymbol {
    public val isPrimary: Boolean
}

public interface KdFunctionSymbol : KdFunctionLikeSymbol {
    public val isSuspend: Boolean
}

public interface KdPropertyLikeSymbol : KdCallableSymbol

// TODO: getter/setter?
public interface KdPropertySymbol : KdPropertyLikeSymbol {
    public val constValue: String?
}

// java field
public interface KdFieldSymbol : KdPropertyLikeSymbol


// for generics
public interface KdTypeParameter : KdDocumented {
    public val name: String
}

// relations should help with merging multiple symbol graphs into one
// for example merging two libraries into a single graph

// inheritance and extensions receiver should be trivial to get
// E.G.
// - find all functions/properties with receiver X
// - find all classes who extend Y
// - find all inheritors of Z

// relations:
// - class<->function
// - class<->property
// - class<->companion
// - class<->constructor
// - class<->class (inheritance)

// - memberOf (function/property/constructor/nestedClass/companionObject?)
// - expectActual
// - inheritance (extends/implements)
//public interface KdRelation {
//
//    public interface Inheritance : KdRelation {
//        public val classReference: KdSymbolReference
//        public val superTypeReference: KdSymbolReference
//    }
//
//    public interface MemberOf : KdRelation {
//        public val classReference: KdSymbolReference
//        public val declarationReference: KdSymbolReference
//    }
//
//    public interface PackageOf : KdRelation {
//        public val packageReference: KdSymbolReference
//        public val declarationReference: KdSymbolReference
//    }
//
//    // TODO - may be not needed
//    public interface ExpectActual : KdRelation {
//        public val expect: KdSymbolReference
//        public val actual: KdSymbolReference
//    }
//}

// in case of expect/actual we will have:
// - class:org.example.HelloWorld@common
// - class:org.example.HelloWorld@jvm
// + relation of type ExpectActual
