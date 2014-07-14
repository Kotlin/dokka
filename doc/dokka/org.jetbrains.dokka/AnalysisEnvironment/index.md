---
layout: post
title: AnalysisEnvironment
---
[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [AnalysisEnvironment](index.md)

# AnalysisEnvironment
Kotlin as a service entry point
```
public class AnalysisEnvironment
```
## Description
```
public class AnalysisEnvironment
```
Configures environment, analyses files and provides facilities to perform code processing without emitting bytecode

**messageCollector**
is required by compiler infrastructure and will receive all compiler messages

**body**
is optional and can be used to configure environment without creating local variable

## Members
| Name | Summary |
|------|---------|
|[*.init*](_init_.md)|Kotlin as a service entry point<br>&nbsp;&nbsp;`public AnalysisEnvironment(messageCollector: MessageCollector, body: AnalysisEnvironment.()->Unit)`<br>|
|[addClasspath](addClasspath.md)|Adds list of paths to classpath.<br>&nbsp;&nbsp;`public fun addClasspath(paths: List<File>): Unit`<br><br>Adds path to classpath.<br>&nbsp;&nbsp;`public fun addClasspath(path: File): Unit`<br>|
|[addSources](addSources.md)|Adds list of paths to source roots.<br>&nbsp;&nbsp;`public fun addSources(list: List<String>): Unit`<br>|
|[classpath](classpath/index.md)|Classpath for this environment.<br>&nbsp;&nbsp;`public val classpath: List<File>`<br>|
|[configuration](configuration.md)|&nbsp;&nbsp;`val configuration: CompilerConfiguration`<br>|
|[dispose](dispose.md)|Disposes the environment and frees all associated resources.<br>&nbsp;&nbsp;`open public fun dispose(): Unit`<br>|
|[messageCollector](messageCollector.md)|&nbsp;&nbsp;`val messageCollector: MessageCollector`<br>|
|[processFiles](processFiles.md)|Runs [processor] for each file and collects its results into single list<br>&nbsp;&nbsp;`public fun <T> processFiles(processor: (BindingContext, JetFile)->T): List<T>`<br>&nbsp;&nbsp;`public fun <T> processFiles(processor: (BindingContext, ModuleDescriptor, JetFile)->T): List<T>`<br>|
|[processFilesFlat](processFilesFlat.md)|Runs [processor] for each file and collects its results into single list<br>&nbsp;&nbsp;`public fun <T> processFilesFlat(processor: (BindingContext, JetFile)->List<T>): List<T>`<br>|
|[sources](sources/index.md)|List of source roots for this environment.<br>&nbsp;&nbsp;`public val sources: List<String>`<br>|
|[streamFiles](streamFiles.md)|Streams files into [processor] and returns a stream of its results<br>&nbsp;&nbsp;`public fun <T> streamFiles(processor: (BindingContext, JetFile)->T): Stream<T>`<br>|
|[withContext](withContext.md)|Executes [processor] when analysis is complete.<br>&nbsp;&nbsp;`public fun <T> withContext(processor: (JetCoreEnvironment, ModuleDescriptor, BindingContext)->T): T`<br>&nbsp;&nbsp;`public fun <T> withContext(processor: (ModuleDescriptor, BindingContext)->T): T`<br>|
