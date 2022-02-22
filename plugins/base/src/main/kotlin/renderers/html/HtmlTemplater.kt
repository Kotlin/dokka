package org.jetbrains.dokka.base.renderers.html

import freemarker.cache.ClassTemplateLoader
import freemarker.cache.FileTemplateLoader
import freemarker.cache.MultiTemplateLoader
import freemarker.log.Logger
import freemarker.template.Configuration
import freemarker.template.Template
import freemarker.template.TemplateExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import java.io.StringWriter


enum class DokkaTemplateTypes(val path: String) {
    BASE("base.ftl")
}

typealias TemplateMap = Map<String, Any?>

class HtmlTemplater(
    context: DokkaContext
) {

    init {
        // to disable logging, but it isn't reliable see [Logger.SYSTEM_PROPERTY_NAME_LOGGER_LIBRARY]
        // (use SLF4j further)
        System.setProperty(
            Logger.SYSTEM_PROPERTY_NAME_LOGGER_LIBRARY,
            System.getProperty(Logger.SYSTEM_PROPERTY_NAME_LOGGER_LIBRARY) ?: Logger.LIBRARY_NAME_NONE
        )
    }

    private val configuration = configuration<DokkaBase, DokkaBaseConfiguration>(context)
    private val templaterConfiguration =
        Configuration(Configuration.VERSION_2_3_31).apply { configureTemplateEngine() }

    private fun Configuration.configureTemplateEngine() {
        val loaderFromResources = ClassTemplateLoader(javaClass, "/dokka/templates")
        templateLoader = configuration?.templatesDir?.let {
            MultiTemplateLoader(
                arrayOf(
                    FileTemplateLoader(it),
                    loaderFromResources
                )
            )
        } ?: loaderFromResources

        unsetLocale()
        defaultEncoding = "UTF-8"
        templateExceptionHandler = TemplateExceptionHandler.RETHROW_HANDLER
        logTemplateExceptions = false
        wrapUncheckedExceptions = true
        fallbackOnNullLoopVariable = false
    }

    fun setupSharedModel(model: TemplateMap) {
        templaterConfiguration.setSharedVariables(model)
    }

    private val cachedTemplates: MutableMap<DokkaTemplateTypes, Template> = mutableMapOf()

    fun renderFromTemplate(
        templateType: DokkaTemplateTypes,
        generateModel: () -> TemplateMap
    ): String {
        val cachedTemplate = cachedTemplates[templateType]
        val out = StringWriter()
        if (cachedTemplate == null) {
            runBlocking {
                val templateDeferred = async { templaterConfiguration.getTemplate(templateType.path) }
                val model = generateModel()
                val template = templateDeferred.await()
                cachedTemplates[templateType] = template
                template.process(model, out)

            }
        } else {
            val model = generateModel()
            cachedTemplate.process(model, out)
        }
        return out.toString()
    }
}

