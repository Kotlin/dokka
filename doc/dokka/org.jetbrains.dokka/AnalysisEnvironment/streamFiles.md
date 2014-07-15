---
layout: api
title: streamFiles
---
[dokka](../../index.html) / [org.jetbrains.dokka](../index.html) / [AnalysisEnvironment](index.html) / [streamFiles](streamFiles.html)


# streamFiles

Streams files into [processor] and returns a stream of its results
```
public fun <T> streamFiles(processor: (BindingContext, JetFile)->T): Stream<T>
```

# Description

```
public fun <T> streamFiles(processor: (BindingContext, JetFile)->T): Stream<T>
```


**processor**
is a function to receive context for symbol resolution and file for processing

