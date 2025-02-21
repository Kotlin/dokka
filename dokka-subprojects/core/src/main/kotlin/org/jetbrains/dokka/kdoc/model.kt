/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kdoc

// declaration hierarchy
// - module   (kotlinx-coroutines-core)
// - fragment (commonMain)
// - package  (kotlinx.coroutines) | file (Coroutines.kt) - meta info?
// - declaration
// - nested declarations

// question to uklibs
// module = org.jetbrains.kotlinx:kotlinx-coroutines-core
// version = 1.0

// THERE IS NO MODULE DEFINITION NOW!
// probably module should be package manager specific

// module                 :fragment:package           :class:member:member
// kotlinx-coroutines-core:common  :kotlinx.coroutines:A    :CLS   :func

// - function:org.example:String.functionWithContextParameters()
// - function:org.example.HelloWorld:String.functionWithContextParameters()
// - function:org.example.HelloWorld:Int.functionWithContextParameters()

// kotlinx.serialization - package
//                       - class
// findPolymorphicSerializer - function
// kotlinx.serialization.internal.AbstractPolymorphicSerializer - receiver
// [TypeParam(bounds=[kotlin.Any])] - receiver generics
// #kotlinx.serialization.encoding.CompositeDecoder - parameter 1
// #kotlin.String? - parameter 2
// /PointingToDeclaration/

public interface KdSymbolReference
public interface KdTypeReference {
    // TODO: functional types
    public val isNullable: Boolean
    public val symbolReference: KdSymbolReference
    public val typeParameters: List<
            KdTypeParameter // + star projection
            >
}

// a.k.a USR
public interface KdSymbolIdentifier

// package:XXX
public interface KdPackageIdentifier : KdSymbolIdentifier {
    public val packageName: String
}

// class:XXX.YYY.ZZZ
public interface KdClassIdentifier : KdSymbolIdentifier {
    public val packageName: String
    public val classNames: List<String> // class + inner classes, nonEmpty
}

// fun/val/var:XXX.YYY.ZZZ.aaa|bbb|ccc|ddd
public interface KdCallableIdentifier : KdSymbolIdentifier {
    public val packageName: String
    public val classNames: List<String> // class + inner classes, nonEmpty

    public val callableName: String // aaa
    public val receiverParameter: KdType? // bbb
    public val valueParameters: List<KdType> // ccc
    public val contextParameters: List<KdType> // ddd
}

// java and kotlin
public sealed class KdVisibility {
    public object Public : KdVisibility()
}

public sealed class KdSourceLanguage {
    public object Kotlin : KdSourceLanguage()
    public object Java : KdSourceLanguage()
}

public interface KdSourceInfo {
    public val path: String // TODO?
    public val language: KdSourceLanguage
    // sourceLink to GH
}

public interface KdAnnotation {
    public val annotationId: KdSymbolReference
    public val parameters: List<KdAnnotationParameter>
}

public interface KdAnnotationParameter {
    public val name: String
    public val type: KdType // TODO: constant type?
    public val value: String
}

// graph

public interface KdSymbolGraph {
    public val schemaVersion: String

    public val symbols: Map<KdSymbolReference, KdSymbol>
    public val relations: List<KdRelation>
}

public interface KdSymbol : KdDocumentable {
    public val identifier: KdSymbolIdentifier
    public val name: String // display, original name from code
}

// other possible symbols:
// - DocSymbol (separate doc)
// - SampleSymbol
// - ModuleSymbol
// - FragmentSymbol (sourceSet)

public interface KdPackageSymbol : KdSymbol // kotlinx.coroutines
public interface KdModuleSymbol : KdSymbol // kotlinx-coroutines-core
public interface KdProjectSymbol : KdSymbol // kotlinx-coroutines


public interface KdSampleSymbol : KdSymbol
public interface KdDocumentationFileSymbol : KdSymbol

// local - TBD
public interface KdFile {
    public val id: String
    public val path: String
    public val remotePath: String
    // link to `sources.jar`/`sources.uklib`
    // link to GH
}

public interface KdSource {
    public val fileId: String
}

public interface KdModule {
    public val packages: List<KdPackage>
    public val fragments: List<KdFragment>
    public val files: List<KdFile> // optional
    public val symbols: Map<KdSymbolReference, KdSymbol>
}

public interface KdPackage : KdDocumentable {
    public val name: String
}

public interface KdFragment {
    public val name: String
    public val dependsOn: Set<String> // fragment names
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

public interface KdEnumEntry : KdDocumentable {
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

public interface KdReturn : KdDocumentable {
    public val type: KdType
}

// parameter = receiver, context, value
public interface KdParameter : KdDocumentable {
    public val type: KdType
}

public interface KdNamedParameter : KdParameter {
    public val name: String
}

public interface KdReceiverParameter : KdParameter
public interface KdContextParameter : KdNamedParameter
public interface KdValueParameter : KdNamedParameter {
    public val defaultValue: String?
}

// for generics
public interface KdTypeParameter : KdDocumentable {
    public val name: String
}

public interface KdType

// parsed markdown representation: text, links, etc
public interface KdDocumentation

public interface KdTag : KdDocumentable {
    public val name: String
}

public interface KdDocumentable {
    public val description: KdDocumentation
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
public interface KdRelation {

    public interface Inheritance : KdRelation {
        public val classReference: KdSymbolReference
        public val superTypeReference: KdSymbolReference
    }

    public interface MemberOf : KdRelation {
        public val classReference: KdSymbolReference
        public val declarationReference: KdSymbolReference
    }

    public interface PackageOf : KdRelation {
        public val packageReference: KdSymbolReference
        public val declarationReference: KdSymbolReference
    }

    // TODO - may be not needed
    public interface ExpectActual : KdRelation {
        public val expect: KdSymbolReference
        public val actual: KdSymbolReference
    }
}
