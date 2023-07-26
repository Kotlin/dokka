package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtNamedSymbol

class TranslatorError(message: String, cause: Throwable?) : IllegalStateException(message, cause)

inline fun <R> KtAnalysisSession.withExceptionCatcher(symbol: KtSymbol, action: KtAnalysisSession.() -> R): R =
    try {
        action()
    } catch (e: TranslatorError) {
        throw e
    } catch (e: Throwable) {
        val file = try {
            symbol.psi?.containingFile?.virtualFile?.path
        } catch (e: Throwable) {
            "[$e]"
        }
        val textRange = try {
            symbol.psi?.textRange.toString()
        } catch (e: Throwable) {
            "[$e]"
        }
        throw TranslatorError(
            "Error in translating of symbol (${(symbol as? KtNamedSymbol)?.name}) $symbol in file: $file, $textRange",
            e
        )
    }