/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

public interface KdClassLike : KdDeclaration

// interface, object, class, annotation, enum
public interface KdClass : KdClassLike {
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

    public val samples: List<KdDeclarationId>
    public val tags: List<KdTag>
}

public interface KdObject : KdClass

public interface KdEnum : KdClass {
    public val entries: List<KdDeclarationId>
}

public interface KdEnumEntry : KdDeclaration {
    public val ordinal: Int
}

public interface KdTypeAlias : KdClassLike {
    public val typeParameters: List<KdTypeParameter>
    public val underlyingType: KdType
}

public interface KdCallable : KdDeclaration {
    public val returnType: KdType

    public val receiverParameter: KdReceiverParameter?
    public val contextParameters: List<KdContextParameter>
}

public interface KdFunctionLike : KdCallable {
    public val valueParameters: List<KdValueParameter>
}

public interface KdConstructor : KdFunctionLike {
    public val isPrimary: Boolean
}

public interface KdFunction : KdFunctionLike {
    public val isSuspend: Boolean
}

public interface KdPropertyLike : KdCallable

// TODO: getter/setter?
public interface KdProperty : KdPropertyLike {
    public val constValue: String?
}

// java field
public interface KdField : KdPropertyLike


// for generics
public interface KdTypeParameter : KdDocumented {
    public val name: String
}
