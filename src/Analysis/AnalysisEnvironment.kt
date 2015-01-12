package org.jetbrains.dokka

import org.jetbrains.kotlin.cli.common.messages.*
import com.intellij.openapi.*
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.psi.*
import java.io.File
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.jvm.*
import com.intellij.openapi.util.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

/**
 * Kotlin as a service entry point
 *
 * Configures environment, analyses files and provides facilities to perform code processing without emitting bytecode
 *
 * $messageCollector: required by compiler infrastructure and will receive all compiler messages
 * $body: optional and can be used to configure environment without creating local variable
 */
public class AnalysisEnvironment(val messageCollector: MessageCollector, body: AnalysisEnvironment.() -> Unit = {}) : Disposable {
    val configuration = CompilerConfiguration();

    {
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        body()
    }

    /**
     * Executes [processor] when analysis is complete.
     * $processor: function to receive compiler environment, module and context for symbol resolution
     */
    public fun withContext<T>(processor: (JetCoreEnvironment, ResolveSession) -> T): T {
        val environment = JetCoreEnvironment.createForProduction(this, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val resolveSession = environment.analyze()
        resolveSession.forceResolveAll()
        return processor(environment, resolveSession)
    }

    /**
     * Executes [processor] when analysis is complete.
     * $processor: function to receive compiler module and context for symbol resolution
     */
    public fun withContext<T>(processor: (ResolveSession) -> T): T {
        return withContext { environment, session -> processor(session) }
    }

    /**
     * Streams files into [processor] and returns a stream of its results
     * $processor: function to receive context for symbol resolution and file for processing
     */
    public fun streamFiles<T>(processor: (ResolveSession, JetFile) -> T): Stream<T> {
        return withContext { environment, session ->
            environment.getSourceFiles().stream().map { file -> processor(session, file) }
        }
    }

    /**
     * Runs [processor] for each file and collects its results into single list
     * $processor: function to receive context for symbol resolution and file for processing
     */
    public fun processFiles<T>(processor: (ResolveSession, JetFile) -> T): List<T> {
        return withContext { environment, session ->
            environment.getSourceFiles().map { file -> processor(session, file) }
        }
    }

    /**
     * Runs [processor] for each file and collects its results into single list
     * $processor: is a function to receive context for symbol resolution and file for processing
     */
    public fun processFilesFlat<T>(processor: (ResolveSession, JetFile) -> List<T>): List<T> {
        return withContext { environment, session ->
            environment.getSourceFiles().flatMap { file -> processor(session, file) }
        }
    }

    /**
     * Classpath for this environment.
     */
    public val classpath: List<File>
        get() = configuration.get(JVMConfigurationKeys.CLASSPATH_KEY) ?: listOf()

    /**
     * Adds list of paths to classpath.
     * $paths: collection of files to add
     */
    public fun addClasspath(paths: List<File>) {
        configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, paths)
    }

    /**
     * Adds path to classpath.
     * $path: path to add
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
     * $list: collection of files to add
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