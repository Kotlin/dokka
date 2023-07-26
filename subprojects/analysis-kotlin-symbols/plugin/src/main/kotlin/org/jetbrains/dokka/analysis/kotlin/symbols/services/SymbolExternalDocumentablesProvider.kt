package org.jetbrains.dokka.analysis.kotlin.symbols.services

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.SymbolsAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.getPsiFilesFromPaths
import org.jetbrains.dokka.analysis.kotlin.symbols.plugin.getSourceFilePaths
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.DokkaSymbolVisitor
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getClassIdFromDRI
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.getDRIFromSymbol
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DClasslike
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.plugin
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.kotlin.internal.ExternalDocumentablesProvider
import org.jetbrains.kotlin.psi.KtFile

internal class SymbolExternalDocumentablesProvider(val context: DokkaContext) : ExternalDocumentablesProvider {
    private val kotlinAnalysis = context.plugin<SymbolsAnalysisPlugin>().querySingle { kotlinAnalysis }

    override fun findClasslike(dri: DRI, sourceSet: DokkaSourceSet): DClasslike? {
        val classId = getClassIdFromDRI(dri)

        // TODO research another ways to get AnalysisSession
        val analysisContext = kotlinAnalysis[sourceSet]
        val someKtFile = getPsiFilesFromPaths<KtFile>(
            analysisContext.project,
            getSourceFilePaths(sourceSet.sourceRoots.map { it.canonicalPath })
        ).first()
        analyze(someKtFile) {
            val symbol = getClassOrObjectSymbolByClassId(classId) as? KtNamedClassOrObjectSymbol?: return null
            val translator = DokkaSymbolVisitor(sourceSet, sourceSet.displayName, analysisContext, logger = context.logger)

            val parentDRI = symbol.getContainingSymbol()?.let { getDRIFromSymbol(it) } ?: /* top level */ DRI(dri.packageName)
            with(translator) {
                return visitNamedClassOrObjectSymbol(symbol, parentDRI)
            }
        }
    }
}
