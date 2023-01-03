package org.jetbrains.dokka.html.renderers.innerTemplating

class DefaultTemplateModelMerger : TemplateModelMerger {
    override fun invoke(
        factories: List<TemplateModelFactory>,
        buildModel: TemplateModelFactory.() -> TemplateMap
    ): TemplateMap {
        val mapper = mutableMapOf<String, Any?>()
        factories.map(buildModel).forEach { partialModel ->
            partialModel.forEach { (k, v) ->
                mapper[k] = v
            }
        }
        return mapper
    }
}