package org.jetbrains.dokka.base.renderers.html.innerTemplating

fun interface TemplateModelMerger {
    fun invoke(factories: List<TemplateModelFactory>, buildModel: TemplateModelFactory.() -> TemplateMap): TemplateMap
}