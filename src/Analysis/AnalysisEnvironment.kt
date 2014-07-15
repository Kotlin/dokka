package org.jetbrains.dokka

import org.jetbrains.jet.cli.common.messages.*
import com.intellij.openapi.*
import org.jetbrains.jet.cli.jvm.compiler.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import java.io.File
import org.jetbrains.jet.config.*
import org.jetbrains.jet.cli.common.*
import org.jetbrains.jet.cli.jvm.*
import com.intellij.openapi.util.*
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor

/**
 * Kotlin as a service entry point
 * Configures environment, analyses files and provides facilities to perform code processing without emitting bytecode
 * $messageCollector is required by compiler infrastructure and will receive all compiler messages
 * $body is optional and can be used to configure environment without creating local variable
 */
public class AnalysisEnvironment(val messageCollector: MessageCollector, body: AnalysisEnvironment.() -> Unit = {}) : Disposable {
    val configuration = CompilerConfiguration();

    {
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        body()
    }

    /**
     * Executes [processor] when analysis is complete.
     * $processor is a function to receive compiler environment, module and context for symbol resolution
     */
    public fun withContext<T>(processor: (JetCoreEnvironment, ModuleDescriptor, BindingContext) -> T): T {
        val environment = JetCoreEnvironment.createForProduction(this, configuration)
        val exhaust = environment.analyze2(messageCollector)
        return processor(environment, exhaust.getModuleDescriptor(), exhaust.getBindingContext())
    }

    /**
     * Executes [processor] when analysis is complete.
     * $processor is a function to receive compiler module and context for symbol resolution
     */
    public fun withContext<T>(processor: (ModuleDescriptor, BindingContext) -> T): T {
        return withContext { environment, module, context -> processor(module, context) }
    }

    /**
     * Streams files into [processor] and returns a stream of its results
     * $processor is a function to receive context for symbol resolution and file for processing
     */
    public fun streamFiles<T>(processor: (BindingContext, JetFile) -> T): Stream<T> {
        return withContext { environment, module, context ->
            environment.getSourceFiles().stream().map { file -> processor(context, file) }
        }
    }

    /**
     * Runs [processor] for each file and collects its results into single list
     * $processor is a function to receive context for symbol resolution and file for processing
     */
    public fun processFiles<T>(processor: (BindingContext, JetFile) -> T): List<T> {
        return withContext { environment, module, context ->
            environment.getSourceFiles().map { file -> processor(context, file) }
        }
    }

    /**
     * Runs [processor] for each file and collects its results into single list
     * $processor is a function to receive context for symbol resolution, module and file for processing
     */
    public fun processFiles<T>(processor: (BindingContext, ModuleDescriptor, JetFile) -> T): List<T> {
        return withContext { environment, module, context ->
            environment.getSourceFiles().map { file -> processor(context, module, file) }
        }
    }

    /**
     * Runs [processor] for each file and collects its results into single list
     * $processor is a function to receive context for symbol resolution and file for processing
     */
    public fun processFilesFlat<T>(processor: (BindingContext, JetFile) -> List<T>): List<T> {
        return withContext { environment, module, context ->
            environment.getSourceFiles().flatMap { file -> processor(context, file) }
        }
    }

    /**
     * Classpath for this environment.
     */
    public val classpath: List<File>
        get() = configuration.get(JVMConfigurationKeys.CLASSPATH_KEY) ?: listOf()

    /**
     * Adds list of paths to classpath.
     * $paths collection of paths to add
     */
    public fun addClasspath(paths: List<File>) {
        configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, paths)
    }

    /**
     * Adds path to classpath.
     * $path path to add
     */
    public fun addClasspath(path: File) {
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, path)
    }

    /**
     * List of source roots for this environment.
     */
    public val sources: List<String>
        get() = configuration.get(CommonConfigurationKeys.SOURCE_ROOTS_KEY) ?: listOf()

    /**
     * Adds list of paths to source roots.
     * $list collection of files to add
     */
    public fun addSources(list: List<String>) {
        configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, list)
    }

    /**
     * Disposes the environment and frees all associated resources.
     */
    public override fun dispose() {
        Disposer.dispose(this)
    }
}