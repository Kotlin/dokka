package com.jetbrains.dokka

import org.jetbrains.jet.cli.common.arguments.*
import org.jetbrains.jet.cli.common.messages.*
import org.jetbrains.jet.cli.jvm.*
import org.jetbrains.jet.cli.jvm.compiler.*
import org.jetbrains.jet.utils.*
import java.io.*
import org.jetbrains.jet.lang.resolve.java.*
import com.google.common.base.*
import com.intellij.psi.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.analyzer.*

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

fun BindingContext.getPackageFragment(file: JetFile) = get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file)
