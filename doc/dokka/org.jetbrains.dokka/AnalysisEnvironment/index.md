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
|[*.init*](_init_.md)|Kotlin as a service entry point<br>`public AnalysisEnvironment(messageCollector: MessageCollector, body: AnalysisEnvironment.()->Unit)`<br>|
|[addClasspath](addClasspath.md)|Adds list of paths to classpath.<br>`public fun addClasspath(paths: List<File>): Unit`<br><br>Adds path to classpath.<br>`public fun addClasspath(path: File): Unit`<br>|
|[addSources](addSources.md)|Adds list of paths to source roots.<br>`public fun addSources(list: List<String>): Unit`<br>|
|[classpath](classpath/index.md)|Classpath for this environment.<br>`public val classpath: List<File>`<br>|
|[configuration](configuration.md)|`val configuration: CompilerConfiguration`<br>|
|[dispose](dispose.md)|Disposes the environment and frees all associated resources.<br>`open public fun dispose(): Unit`<br>|
|[messageCollector](messageCollector.md)|`val messageCollector: MessageCollector`<br>|
|[processFiles](processFiles.md)|Runs [processor] for each file and collects its results into single list<br>`public fun <T> processFiles(processor: (BindingContext, JetFile)->T): List<T>`<br><br>Runs [processor] for each file and collects its results into single list<br>`public fun <T> processFiles(processor: (BindingContext, ModuleDescriptor, JetFile)->T): List<T>`<br>|
|[processFilesFlat](processFilesFlat.md)|Runs [processor] for each file and collects its results into single list<br>`public fun <T> processFilesFlat(processor: (BindingContext, JetFile)->List<T>): List<T>`<br>|
|[sources](sources/index.md)|List of source roots for this environment.<br>`public val sources: List<String>`<br>|
|[streamFiles](streamFiles.md)|Streams files into [processor] and returns a stream of its results<br>`public fun <T> streamFiles(processor: (BindingContext, JetFile)->T): Stream<T>`<br>|
|[withContext](withContext.md)|Executes [processor] when analysis is complete.<br>`public fun <T> withContext(processor: (JetCoreEnvironment, ModuleDescriptor, BindingContext)->T): T`<br><br>Executes [processor] when analysis is complete.<br>`public fun <T> withContext(processor: (ModuleDescriptor, BindingContext)->T): T`<br>|
