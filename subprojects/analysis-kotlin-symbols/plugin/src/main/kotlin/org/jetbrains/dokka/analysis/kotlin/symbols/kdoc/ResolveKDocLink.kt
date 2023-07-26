package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement

/**
 *  Callable have priority independent of scope order
 */
private fun KtAnalysisSession.searchInScope(scope: KtScope, inndexOfLinkSegment: Int, linkSegments: List<Name>): KtSymbol? {
    val identifier = linkSegments[inndexOfLinkSegment]
    val isLastPart = inndexOfLinkSegment == linkSegments.size - 1
    if(isLastPart) {
        val callableSymbols = scope.getCallableSymbols { name -> name == identifier }
        callableSymbols.firstOrNull()?.let { return it }
    }

    val classifierSymbols = scope.getClassifierSymbols { name -> name == identifier }
    if(isLastPart) classifierSymbols.firstOrNull()?.let{ return it }
    val packageSymbols = scope.getPackageSymbols { name -> name == identifier }
    if(isLastPart) return packageSymbols.firstOrNull()

    val packageAndClassifierScopes = packageSymbols.map { it.getPackageScope() } + classifierSymbols.filterIsInstance<KtSymbolWithMembers>().map { it.getDeclaredMemberScope() }
    val compositeScope = packageAndClassifierScopes.toList().asCompositeScope()

    return searchInScope(compositeScope, inndexOfLinkSegment + 1, linkSegments)
}
internal fun KtAnalysisSession.resolveKDocLink(link: String, contextSymbol: KtSymbol) =
    (contextSymbol.psi as? KtElement)?.let { ktElement -> resolveKDocLink(link, ktElement) }

internal fun KtAnalysisSession.resolveKDocLink(link: String, contextElement: KtElement): KtSymbol? {
    val linkParts = link.split(".").map {  Name.identifier(it) }

    val  file = contextElement.containingKtFile
    val scope =  file.getScopeContextForPosition(contextElement).getCompositeScope()

    return searchInScope(scope, 0, linkParts) // relative link
        ?: searchInScope(ROOT_PACKAGE_SYMBOL.getPackageScope(), 0, linkParts) // fq link
}