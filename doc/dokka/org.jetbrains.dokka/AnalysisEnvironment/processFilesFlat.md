[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [AnalysisEnvironment](index.md) / [processFilesFlat](processFilesFlat.md)

# processFilesFlat
Runs [processor] for each file and collects its results into single list
```
public fun <T> processFilesFlat(processor: (BindingContext, JetFile)->List<T>): List<T>
```
## Description
```
public fun <T> processFilesFlat(processor: (BindingContext, JetFile)->List<T>): List<T>
```


**processor**
is a function to receive context for symbol resolution and file for processing

