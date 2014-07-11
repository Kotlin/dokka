package com.jetbrains.dokka

import org.jetbrains.jet.cli.common.arguments.*
import com.sampullara.cli.*
import com.intellij.openapi.util.*
import org.jetbrains.jet.cli.common.messages.*
import org.jetbrains.jet.cli.jvm.*
import org.jetbrains.jet.config.*
import org.jetbrains.jet.cli.jvm.compiler.*
import org.jetbrains.jet.cli.common.*
import org.jetbrains.jet.utils.*
import java.io.*
import org.jetbrains.jet.lang.resolve.java.*
import com.google.common.base.*
import com.intellij.psi.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.analyzer.*

public fun main(args: Array<String>) {

    val arguments = K2JVMCompilerArguments()
    arguments.freeArgs = Args.parse(arguments, args)

    val rootDisposable = Disposer.newDisposable()

    val messageCollector = MessageCollectorPlainTextToStream.PLAIN_TEXT_TO_SYSTEM_ERR
    val configuration = CompilerConfiguration()

    val paths = PathUtil.getKotlinPathsForCompiler()

    configuration.addAll(JVMConfigurationKeys.CLASSPATH_KEY, getClasspath(paths, arguments))
    configuration.addAll(CommonConfigurationKeys.SOURCE_ROOTS_KEY, arguments.freeArgs ?: listOf())
    configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)

    val environment = JetCoreEnvironment.createForProduction(rootDisposable, configuration)
    val context = environment.analyze(messageCollector)
    rootDisposable.dispose()
}

private fun getClasspath(paths: KotlinPaths, arguments: K2JVMCompilerArguments): MutableList<File> {
    val classpath = arrayListOf<File>()
    classpath.addAll(PathUtil.getJdkClassesRoots())
    classpath.add(paths.getRuntimePath())
    val classPath = arguments.classpath
    if (classPath != null) {
        for (element in classPath.split(File.pathSeparatorChar)) {
            classpath.add(File(element))
        }
    }
    return classpath
}

private fun getAnnotationsPath(paths: KotlinPaths, arguments: K2JVMCompilerArguments): MutableList<File> {
    val annotationsPath = arrayListOf<File>()
    annotationsPath.add(paths.getJdkAnnotationsPath())
    val annotationPaths = arguments.annotations
    if (annotationPaths != null) {
        for (element in annotationPaths.split(File.pathSeparatorChar)) {
            annotationsPath.add(File(element))
        }
    }
    return annotationsPath
}

private fun JetCoreEnvironment.analyze(messageCollector: MessageCollector): BindingContext {
    val project = getProject()
    val sourceFiles = getSourceFiles()

    val analyzerWithCompilerReport = AnalyzerWithCompilerReport(messageCollector)
    analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
        val support = CliLightClassGenerationSupport.getInstanceForCli(project)!!
        val sharedTrace = support.getTrace()
        val sharedModule = support.getModule()
        val compilerConfiguration = getConfiguration()!!
        AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(project, sourceFiles, sharedTrace,
                                                             Predicates.alwaysTrue<PsiFile>(),
                                                             sharedModule,
                                                             compilerConfiguration.get(JVMConfigurationKeys.MODULE_IDS),
                                                             compilerConfiguration.get(JVMConfigurationKeys.INCREMENTAL_CACHE_BASE_DIR))
    }

    val exhaust = analyzerWithCompilerReport.getAnalyzeExhaust()
    assert(exhaust != null) { "AnalyzeExhaust should be non-null, compiling: " + sourceFiles }

    return exhaust!!.getBindingContext()
}

fun AnalyzerWithCompilerReport.analyzeAndReport(files: List<JetFile>, analyser: () -> AnalyzeExhaust) = analyzeAndReport(analyser, files)