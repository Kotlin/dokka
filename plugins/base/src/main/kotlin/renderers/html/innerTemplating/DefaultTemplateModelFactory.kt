package org.jetbrains.dokka.base.renderers.html.innerTemplating

import freemarker.core.Environment
import freemarker.template.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import org.jetbrains.dokka.base.renderers.URIExtension
import org.jetbrains.dokka.base.renderers.html.TEMPLATE_REPLACEMENT
import org.jetbrains.dokka.base.renderers.html.command.consumers.ImmediateResolutionTagConsumer
import org.jetbrains.dokka.base.renderers.html.templateCommand
import org.jetbrains.dokka.base.renderers.html.templateCommandAsHtmlComment
import org.jetbrains.dokka.base.renderers.isImage
import org.jetbrains.dokka.base.resolvers.local.LocationProvider
import org.jetbrains.dokka.base.templating.PathToRootSubstitutionCommand
import org.jetbrains.dokka.base.templating.ProjectNameSubstitutionCommand
import org.jetbrains.dokka.base.templating.ReplaceVersionsCommand
import org.jetbrains.dokka.base.templating.SubstitutionCommand
import org.jetbrains.dokka.model.DisplaySourceSet
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.ContentPage
import org.jetbrains.dokka.pages.PageNode
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.plugability.configuration
import java.net.URI

class DefaultTemplateModelFactory(val context: DokkaContext) : TemplateModelFactory {
    private val configuration = configuration<DokkaBase, DokkaBaseConfiguration>(context)
    private val isPartial = context.configuration.delayTemplateSubstitution

    private fun <R> TagConsumer<R>.prepareForTemplates() =
        if (context.configuration.delayTemplateSubstitution || this is ImmediateResolutionTagConsumer) this
        else ImmediateResolutionTagConsumer(this, context)

    data class SourceSetModel(val name: String, val platform: String, val filter: String)

    override fun buildModel(
        page: PageNode,
        resources: List<String>,
        locationProvider: LocationProvider,
        content: String
    ): TemplateMap {
        val path = locationProvider.resolve(page)
        val pathToRoot = locationProvider.pathToRoot(page)
        val mapper = mutableMapOf<String, Any>()
        mapper["pageName"] = page.name
        mapper["resources"] = PrintDirective {
            val sb = StringBuilder()
            if (isPartial)
                sb.templateCommandAsHtmlComment(
                    PathToRootSubstitutionCommand(
                        TEMPLATE_REPLACEMENT,
                        default = pathToRoot
                    )
                ) { resourcesForPage(TEMPLATE_REPLACEMENT, resources) }
            else
                sb.resourcesForPage(pathToRoot, resources)
            sb.toString()
        }
        mapper["content"] = PrintDirective { content }
        mapper["version"] = PrintDirective {
            createHTML().prepareForTemplates().templateCommand(ReplaceVersionsCommand(path.orEmpty()))
        }
        mapper["template_cmd"] = TemplateDirective(context.configuration, pathToRoot)

        if (page is ContentPage) {
            val sourceSets = page.content.withDescendants()
                .flatMap { it.sourceSets }
                .distinct()
                .sortedBy { it.comparableKey }
                .map { SourceSetModel(it.name, it.platform.key, it.sourceSetIDs.merged.toString()) }
                .toList()

            if (sourceSets.isNotEmpty()) {
                mapper["sourceSets"] = sourceSets
            }
        }
        return mapper
    }

    override fun buildSharedModel(): TemplateMap = mapOf<String, Any>(
        "footerMessage" to (configuration?.footerMessage?.takeIf { it.isNotEmpty() }
            ?: DokkaBaseConfiguration.defaultFooterMessage)
    )

    private val DisplaySourceSet.comparableKey
        get() = sourceSetIDs.merged.let { it.scopeId + it.sourceSetName }
    private val String.isAbsolute: Boolean
        get() = URI(this).isAbsolute

    private fun Appendable.resourcesForPage(pathToRoot: String, resources: List<String>): Unit =
        resources.forEach {
            append(with(createHTML()) {
                when {
                    it.URIExtension == "css" ->
                        link(
                            rel = LinkRel.stylesheet,
                            href = if (it.isAbsolute) it else "$pathToRoot$it"
                        )
                    it.URIExtension == "js" ->
                        script(
                            type = ScriptType.textJavaScript,
                            src = if (it.isAbsolute) it else "$pathToRoot$it"
                        ) {
                            if (it == "scripts/main.js" || it.endsWith("_deferred.js"))
                                defer = true
                            else
                                async = true
                        }
                    it.isImage() -> link(href = if (it.isAbsolute) it else "$pathToRoot$it")
                    else -> null
                }
            } ?: it)
        }
}

private class PrintDirective(val generateData: () -> String) : TemplateDirectiveModel {
    override fun execute(
        env: Environment,
        params: MutableMap<Any?, Any?>?,
        loopVars: Array<TemplateModel>?,
        body: TemplateDirectiveBody?
    ) {
        if (params?.isNotEmpty() == true) throw TemplateModelException(
            "Parameters are not allowed"
        )
        if (loopVars?.isNotEmpty() == true) throw TemplateModelException(
            "Loop variables are not allowed"
        )
        env.out.write(generateData())
    }
}

private class TemplateDirective(val configuration: DokkaConfiguration, val pathToRoot: String) : TemplateDirectiveModel {
    override fun execute(
        env: Environment,
        params: MutableMap<Any?, Any?>?,
        loopVars: Array<TemplateModel>?,
        body: TemplateDirectiveBody?
    ) {
        val commandName = params?.get(PARAM_NAME) ?: throw TemplateModelException(
            "The required $PARAM_NAME parameter is missing."
        )
        val replacement = (params[PARAM_REPLACEMENT] as? SimpleScalar)?.asString ?: TEMPLATE_REPLACEMENT

        when ((commandName as? SimpleScalar)?.asString) {
            "pathToRoot" -> {
                body ?: throw TemplateModelException(
                    "No directive body for $commandName command."
                )
                executeSubstituteCommand(
                    PathToRootSubstitutionCommand(
                        replacement, pathToRoot
                    ),
                    "pathToRoot",
                    pathToRoot,
                    Context(env, body)
                )
            }
            "projectName" -> {
                body ?: throw TemplateModelException(
                    "No directive body $commandName command."
                )
                executeSubstituteCommand(
                    ProjectNameSubstitutionCommand(
                        replacement, configuration.moduleName
                    ),
                    "projectName",
                    configuration.moduleName,
                    Context(env, body)
                )
            }
            else -> throw TemplateModelException(
                "The parameter $PARAM_NAME $commandName is unknown"
            )
        }
    }

    private data class Context(val env: Environment, val body: TemplateDirectiveBody)

    private fun executeSubstituteCommand(
        command: SubstitutionCommand,
        name: String,
        value: String,
        ctx: Context
    ) {
        if (configuration.delayTemplateSubstitution)
            ctx.env.out.templateCommandAsHtmlComment(command) {
                renderWithLocalVar(name, command.pattern, ctx)
            }
        else {
            renderWithLocalVar(name, value, ctx)
        }
    }

    private fun renderWithLocalVar(name: String, value: String, ctx: Context) =
        with(ctx) {
            env.setVariable(name, SimpleScalar(value))
            body.render(env.out)
            env.setVariable(name, null)
        }

    companion object {
        const val PARAM_NAME = "name"
        const val PARAM_REPLACEMENT = "replacement"
    }
}
