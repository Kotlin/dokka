package org.jetbrains.dokka.base.translators.symbols

import org.jetbrains.dokka.links.*
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithTypeParameters
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

internal fun ClassId.createDRI(): DRI = DRI(
    packageName = this.packageFqName.asString(), classNames = this.relativeClassName.asString()
)

private fun CallableId.createDRI(receiver: TypeReference?, params: List<TypeReference>): DRI = DRI(
    packageName = this.packageName.asString(),
    classNames = this.className?.asString(),
    callable = Callable(
        this.callableName.asString(),
        params = params,
        receiver = receiver
    )
)

internal fun getDRIFromNonErrorClassType(nonErrorClassType: KtNonErrorClassType): DRI = nonErrorClassType.classId.createDRI()

internal fun getDRIFromEnumEntry(symbol: KtEnumEntrySymbol): DRI =
    symbol.callableIdIfNonLocal?.createDRI(null, emptyList())?.withEnumEntryExtra()
        ?: throw IllegalStateException("")

internal fun KtAnalysisSession.getDRIFromTypeParameter(symbol: KtTypeParameterSymbol): DRI {
    val containingSymbol =
        symbol.getContainingSymbol() ?: throw IllegalStateException("`getContainingSymbol` is null for type parameter")
    val typeParameters = (containingSymbol as? KtSymbolWithTypeParameters)?.typeParameters
    val index = typeParameters?.indexOfFirst { symbol.name == it.name } ?: -1
    return if (index == -1)
        getDRIFromSymbol(containingSymbol)
    else
        getDRIFromSymbol(containingSymbol).copy(target = PointingToGenericParameters(index))
}

internal fun KtAnalysisSession.getDRIFromConstructor(symbol: KtConstructorSymbol): DRI =
    symbol.containingClassIdIfNonLocal?.createDRI()?.copy(
        callable = Callable(
            name = symbol.containingClassIdIfNonLocal?.relativeClassName?.asString() ?: "",
            params = symbol.valueParameters.map { getTypeReferenceFrom(it.returnType) }
        ))
?: throw IllegalStateException("")

internal fun KtAnalysisSession.getDRIFromVariableLike(symbol: KtVariableLikeSymbol): DRI {
    val receiver = symbol.receiverType?.let {// TODO: replace `receiverType` with `receiverParameter`
        getTypeReferenceFrom(it)
    }
    return symbol.callableIdIfNonLocal?.createDRI(receiver, emptyList())
        ?: throw IllegalStateException("")
}

internal fun KtAnalysisSession.getDRIFromFunctionLike(symbol: KtFunctionLikeSymbol): DRI {
    val params = symbol.valueParameters.map { getTypeReferenceFrom(it.returnType) }
    val receiver = symbol.receiverType?.let { // TODO: replace `receiverType` with `receiverParameter`
        getTypeReferenceFrom(it)
    }
    return symbol.callableIdIfNonLocal?.createDRI(receiver, params)
        ?: getDRIFromLocalFunction(symbol)
}

internal fun getDRIFromClassLike(symbol: KtClassLikeSymbol): DRI =
    symbol.classIdIfNonLocal?.createDRI() ?: throw IllegalStateException()

internal fun KtAnalysisSession.getDRIFromSymbol(symbol: KtSymbol): DRI =
    when (symbol) {
        is KtEnumEntrySymbol -> getDRIFromEnumEntry(symbol)
        is KtTypeParameterSymbol -> getDRIFromTypeParameter(symbol)
        is KtConstructorSymbol -> getDRIFromConstructor(symbol)
        is KtVariableLikeSymbol -> getDRIFromVariableLike(symbol)
        is KtFunctionLikeSymbol -> getDRIFromFunctionLike(symbol)
        is KtClassLikeSymbol -> getDRIFromClassLike(symbol)
        else -> throw IllegalStateException("Unknown symbol while creating DRI ")
    }

private fun KtAnalysisSession.getDRIFromLocalFunction(symbol: KtFunctionLikeSymbol): DRI {
    /**
     * in enum entry: memberSymbol.callableIdIfNonLocal=null
     */
    return getDRIFromSymbol(symbol.getContainingSymbol() as KtSymbol).copy(
        callable = Callable(
            (symbol as? KtNamedSymbol)?.name?.asString() ?: "",
            params = symbol.valueParameters.map { getTypeReferenceFrom(it.returnType) },
            receiver = symbol.receiverType?.let { // TODO: replace `receiverType` with `receiverParameter`
                getTypeReferenceFrom(it)
            }
//                    receiver = symbol.receiverParameter?.let {
//                        getTypeReferenceFrom(it)
//                    }
        )
    )
}
