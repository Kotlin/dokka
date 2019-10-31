package org.jetbrains.dokka

import kotlinx.html.DD
import org.jetbrains.dokka.Model.Module
import org.jetbrains.dokka.Utilities.pretty
import org.jetbrains.dokka.links.DRI
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class DokkaGenerator(
    private val configuration: DokkaConfiguration,
    private val logger: DokkaLogger
) {
    fun generate() = configuration.passesConfigurations.forEach { pass ->
        AnalysisEnvironment(DokkaMessageCollector(logger), pass.analysisPlatform).run {
            if (analysisPlatform == Platform.jvm) {
                addClasspath(PathUtil.getJdkClassesRootsFromCurrentJre())
            }
            for (element in pass.classpath) {
                addClasspath(File(element))
            }

            addSources(pass.sourceRoots.map { it.path })

            loadLanguageVersionSettings(pass.languageVersion, pass.apiVersion)

            val environment = createCoreEnvironment()
            val (facade, _) = createResolutionFacade(environment)

            environment.getSourceFiles().asSequence()
                .map { it.packageFqName }
                .distinct()
                .mapNotNull { facade.resolveSession.getPackageFragment(it) }
                .map { DokkaDescriptorVisitor.visitPackageFragmentDescriptor(it, DRI.topLevel) }
                .toList()
                .let { Module(it) }
        }.also { println("${pass.analysisPlatform}:\n${it.pretty()}\n\n") }
    }


}

private class DokkaMessageCollector(private val logger: DokkaLogger) : MessageCollector {
    override fun clear() {
        seenErrors = false
    }

    private var seenErrors = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageLocation?) {
        if (severity == CompilerMessageSeverity.ERROR) {
            seenErrors = true
        }
        logger.error(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
    }

    override fun hasErrors() = seenErrors
}