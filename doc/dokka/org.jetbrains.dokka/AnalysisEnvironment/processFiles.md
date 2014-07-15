---
layout: api
title: processFiles
---
[dokka](../../index.html) / [org.jetbrains.dokka](../index.html) / [AnalysisEnvironment](index.html) / [processFiles](processFiles.html)


# processFiles

Runs [processor] for each file and collects its results into single list
```
public fun <T> processFiles(processor: (BindingContext, JetFile)->T): List<T>
public fun <T> processFiles(processor: (BindingContext, ModuleDescriptor, JetFile)->T): List<T>
```

# Description

```
public fun <T> processFiles(processor: (BindingContext, JetFile)->T): List<T>
```


**processor**
is a function to receive context for symbol resolution and file for processing

```
public fun <T> processFiles(processor: (BindingContext, ModuleDescriptor, JetFile)->T): List<T>
```


**processor**
is a function to receive context for symbol resolution, module and file for processing

