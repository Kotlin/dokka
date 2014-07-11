package org.jetbrains.dokka

import com.intellij.openapi.*
import org.jetbrains.jet.cli.common.messages.*
import org.jetbrains.jet.cli.common.*
import org.jetbrains.jet.cli.jvm.compiler.*
import java.io.*
import org.jetbrains.jet.cli.jvm.*
import org.jetbrains.jet.config.*
import com.intellij.openapi.util.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import kotlin.platform.*

public class AnalysesEnvironment(val messageCollector: MessageCollector, body: AnalysesEnvironment.() -> Unit = {}) : Disposable {
    val configuration = CompilerConfiguration();

    {
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        body()
    }

    private fun withContext<T>(processor: (JetCoreEnvironment, BindingContext) -> T): T {
        val environment = JetCoreEnvironment.createForProduction(this, configuration)
        val result = environment.analyze(messageCollector)
        return processor(environment, result)
    }

    public fun withContext<T>(processor: (BindingContext) -> T): T {
        return withContext { environment, context -> processor(context) }
    }

    public fun streamFiles<T>(processor: (BindingContext, JetFile) -> T): Stream<T> {
        return withContext { environment, context ->
            environment.getSourceFiles().stream().map { file -> processor(context, file) }
        }
    }

    public fun processFiles<T>(processor: (BindingContext, JetFile) -> T): List<T> {
        return withContext { environment, context ->
            environment.getSourceFiles().map { file -> processor(context, file) }
        }
    }

    public fun processFilesFlat<T>(processor: (BindingContext, JetFile) -> List<T>): List<T> {
        return withContext { environment, context ->
            environment.getSourceFiles().flatMap { file -> processor(context, file) }
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