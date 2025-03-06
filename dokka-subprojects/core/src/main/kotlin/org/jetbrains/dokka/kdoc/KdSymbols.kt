/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

//public interface KdSymbolReference
public interface KdTypeReference {
    // TODO: functional types
    public val isNullable: Boolean
    public val symbolReference: KdSymbolReference
    public val typeParameters: List<
            KdTypeParameter // + star projection
            >
}

// graph

public interface KdSymbols {
    public val symbols: Map<KdSymbolId, KdSymbol>
}

// declaration has both package and fragment
public interface KdDeclarationSymbol : KdSymbol {
    public val visibility: KdVisibility
    public val source: KdSourceInfo

    // we should have access to `MustBeDocumented` annotations
    // annotations?
}

// in case of expect/actual we will have:
// - class:org.example.HelloWorld@common
// - class:org.example.HelloWorld@jvm
// + relation of type ExpectActual

public interface KdClassLikeSymbol : KdSymbol

// interface, object, class, annotation, enum
public interface KdClassSymbol : KdClassLikeSymbol {
    public val superTypes: List<KdType>
    public val classlikes: List<KdSymbolReference>
    public val callables: List<KdSymbolReference>

    // generics?

    // @see - separate section
    // @sample - separate section
    // @author - separate section
    // @since - separate section
    // and other javadoc tags

    public val author: String?
    public val since: String?
    public val samples: List<KdSymbolReference> // ???
}

public interface KdEnumSymbol : KdClassSymbol {
    public val entries: List<KdEnumEntry>
}

public interface KdEnumEntry : KdDocumented {
    public val name: String
}

public interface KdTypeAliasSymbol : KdClassLikeSymbol {
    public val typeParameters: List<KdTypeParameter>
    public val underlyingType: KdType
}

public interface KdCallableSymbol : KdSymbol {
    // should have docs on @return ...
    public val returnType: KdType
    public val returnDescription: KdDocumentation

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

public interface KdPropertySymbol : KdPropertyLikeSymbol {
    public val constValue: String?
}

// java field
public interface KdFieldSymbol : KdPropertyLikeSymbol

public interface KdReturn : KdDocumented {
    public val type: KdType
}

// for generics
public interface KdTypeParameter : KdDocumented {
    public val name: String
}

public interface KdType

public interface KdTag : KdDocumented {
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
