package com.jetbrains.dokka

import com.intellij.openapi.Disposable
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.jet.cli.common.messages.MessageCollector
import org.jetbrains.jet.cli.common.CLIConfigurationKeys
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import java.io.File
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys
import org.jetbrains.jet.config.CommonConfigurationKeys
import com.intellij.openapi.util.Disposer
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetFile

public class AnalysisEnvironment(val messageCollector: MessageCollector, body: AnalysisEnvironment.()->Unit = {}) : Disposable {
    val configuration = CompilerConfiguration();

    {
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        body()
    }

    private fun processContext<T>(processor: (JetCoreEnvironment, BindingContext) -> T): T {
        val environment = JetCoreEnvironment.createForProduction(this, configuration)
        val result = environment.analyze(messageCollector)
        return processor(environment, result)
    }

    public fun processContext<T>(processor: (BindingContext) -> T): T {
        return processContext { environment, context -> processor(context) }
    }

    public fun streamFiles<T>(processor: (BindingContext, JetFile) -> T): Stream<T> {
        return processContext { environment, context ->
            environment.getSourceFiles().stream().map { file -> processor(context, file) }
        }
    }

    public fun processFiles<T>(processor: (BindingContext, JetFile) -> T): List<T> {
        return processContext { environment, context ->
            environment.getSourceFiles().map { file -> processor(context, file) }
        }
    }

    public val classpath: List<File>
        get() = configuration.get(JVMConfigurationKeys.CLASSPATH_KEY) ?: listOf()

    public fun addClasspath(list: List<File>) {
        configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, list)
    }

    public fun addClasspath(file: File) {
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, file)
    }

    public val sources: List<String>
        get() = configuration.get(CommonConfigurationKeys.SOURCE_ROOTS_KEY) ?: listOf()
    public fun addSources(list: List<String>) {
        configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, list)
    }

    public override fun dispose() {
        Disposer.dispose(this)
    }
}