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

public class DokkaContext(val messageCollector: MessageCollector) : Disposable {
    val configuration = CompilerConfiguration()

            ;
    {
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
    }

    private fun analyze<T>(analyser: (JetCoreEnvironment, BindingContext) -> T) {
        val environment = JetCoreEnvironment.createForProduction(this, configuration)
        val result = environment.analyze(messageCollector)
        analyser(environment, result)
    }

    public fun analyze<T>(analyser: (BindingContext) -> T) {
        analyze { environment, context ->
            analyser(context)
        }
    }

    public fun analyzeFiles<T>(analyser: (BindingContext, JetFile) -> T) {
        analyze { environment, context ->
            for (file in environment.getSourceFiles())
                analyser(context, file)
        }
    }

    public val classpath: List<File>
        get() = configuration.get(JVMConfigurationKeys.CLASSPATH_KEY) ?: listOf()

    public fun addClasspath(list: List<File>) {
        configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, list)
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