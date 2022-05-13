package org.jetbrains.dokka.base.transformers.documentables

import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.plugability.DokkaContext

class KotlinArrayDocumentableReplacerTransformer(context: DokkaContext):
    DocumentableReplacerTransformer(context) {

    private fun Documentable.isJVM() =
        sourceSets.any{ it.analysisPlatform == Platform.jvm }

    override fun processGenericTypeConstructor(genericTypeConstructor: GenericTypeConstructor): AnyWithChanges<GenericTypeConstructor> =
        genericTypeConstructor.takeIf { genericTypeConstructor.dri == DRI("kotlin", "Array") }
            ?.let {
                with(it.projections.firstOrNull() as? Variance<Bound>) {
                    with(this?.inner as? GenericTypeConstructor) {
                        when (this?.dri) {
                            DRI("kotlin", "Int") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(
                                        dri = DRI("kotlin", "IntArray"),
                                        projections = emptyList(),
                                        sources = genericTypeConstructor.sources
                                    ),
                                    true)
                            DRI("kotlin", "Boolean") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(
                                        dri = DRI("kotlin", "BooleanArray"),
                                        projections = emptyList(),
                                        sources = genericTypeConstructor.sources
                                    ),
                                    true)
                            DRI("kotlin", "Float") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(
                                        dri = DRI("kotlin", "FloatArray"),
                                        projections = emptyList(),
                                        sources = genericTypeConstructor.sources
                                    ),
                                    true)
                            DRI("kotlin", "Double") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(
                                        dri = DRI("kotlin", "DoubleArray"),
                                        projections = emptyList(),
                                        sources = genericTypeConstructor.sources
                                    ),
                                    true)
                            DRI("kotlin", "Long") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(
                                        dri = DRI("kotlin", "LongArray"),
                                        projections = emptyList(),
                                        sources = genericTypeConstructor.sources),
                                    true)
                            DRI("kotlin", "Short") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(
                                        dri = DRI("kotlin", "ShortArray"),
                                        projections = emptyList(),
                                        sources = genericTypeConstructor.sources),
                                    true)
                            DRI("kotlin", "Char") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(
                                        dri = DRI("kotlin", "CharArray"),
                                        projections = emptyList(),
                                        sources = genericTypeConstructor.sources),
                                    true)
                            DRI("kotlin", "Byte") ->
                                AnyWithChanges(
                                    GenericTypeConstructor(
                                        dri = DRI("kotlin", "ByteArray"),
                                        projections = emptyList(),
                                        sources = genericTypeConstructor.sources),
                                    true)
                            else -> null
                        }
                    }
                }
            }
            ?: super.processGenericTypeConstructor(genericTypeConstructor)

    override fun processModule(module: DModule): AnyWithChanges<DModule>  =
        if(module.isJVM())
            super.processModule(module)
        else AnyWithChanges(module)
}