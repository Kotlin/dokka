---
layout: api
title: AnalysisEnvironment
---
[dokka](../../index.html) / [org.jetbrains.dokka](../index.html) / [AnalysisEnvironment](index.html)


# AnalysisEnvironment

Kotlin as a service entry point

```
public class AnalysisEnvironment
```


### Description

Configures environment, analyses files and provides facilities to perform code processing without emitting bytecode

**messageCollector**
is required by compiler infrastructure and will receive all compiler messages

**body**
is optional and can be used to configure environment without creating local variable


### Members


|[&lt;init&gt;](_init_.html)|Kotlin as a service entry point<br/>**`public AnalysisEnvironment(messageCollector: MessageCollector, body: AnalysisEnvironment.() -> Unit)`**|
|[addClasspath](addClasspath.html)|Adds list of paths to classpath.<br/>**`public fun addClasspath(paths: List<File>): Unit`**Adds path to classpath.<br/>**`public fun addClasspath(path: File): Unit`**|
|[addSources](addSources.html)|Adds list of paths to source roots.<br/>**`public fun addSources(list: List<String>): Unit`**|
|[classpath](classpath/index.html)|Classpath for this environment.<br/>**`public val classpath: List<File>`**|
|[dispose](dispose.html)|Disposes the environment and frees all associated resources.<br/>**`open public fun dispose(): Unit`**|
|[processFiles](processFiles.html)|Runs [processor] for each file and collects its results into single list<br/>**`public fun <T> processFiles(processor: (BindingContext, JetFile) -> T): List<T>`**<br/>**`public fun <T> processFiles(processor: (BindingContext, ModuleDescriptor, JetFile) -> T): List<T>`**|
|[processFilesFlat](processFilesFlat.html)|Runs [processor] for each file and collects its results into single list<br/>**`public fun <T> processFilesFlat(processor: (BindingContext, JetFile) -> List<T>): List<T>`**|
|[sources](sources/index.html)|List of source roots for this environment.<br/>**`public val sources: List<String>`**|
|[streamFiles](streamFiles.html)|Streams files into [processor] and returns a stream of its results<br/>**`public fun <T> streamFiles(processor: (BindingContext, JetFile) -> T): Stream<T>`**|
|[withContext](withContext.html)|Executes [processor] when analysis is complete.<br/>**`public fun <T> withContext(processor: (JetCoreEnvironment, ModuleDescriptor, BindingContext) -> T): T`**<br/>**`public fun <T> withContext(processor: (ModuleDescriptor, BindingContext) -> T): T`**|

