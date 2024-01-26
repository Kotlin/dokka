/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base.renderers.html.innerTemplating

import freemarker.cache.ClassTemplateLoader
import freemarker.cache.FileTemplateLoader
import freemarker.cache.MultiTemplateLoader
import freemarker.log.Logger
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import java.io.StringWriter


public enum class DokkaTemplateTypes(
    public val path: String
) {
    BASE("base.ftl")
}

public typealias TemplateMap = Map<String, Any?>

public class HtmlTemplater(
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
        templateUpdateDelayMilliseconds = Long.MAX_VALUE
    }

    public fun setupSharedModel(model: TemplateMap) {
        templaterConfiguration.setSharedVariables(model)
    }

    public fun renderFromTemplate(
        templateType: DokkaTemplateTypes,
        generateModel: () -> TemplateMap
    ): String {
        val out = StringWriter()
        // Freemarker has own thread-safe cache to keep templates
        val template = templaterConfiguration.getTemplate(templateType.path)
        val model = generateModel()
        template.process(model, out)

        return out.toString()
    }
}

