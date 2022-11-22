package org.jetbrains.dokka.html.renderers.innerTemplating

fun interface TemplateModelMerger {
    fun invoke(factories: List<TemplateModelFactory>, buildModel: TemplateModelFactory.() -> TemplateMap): TemplateMap
}