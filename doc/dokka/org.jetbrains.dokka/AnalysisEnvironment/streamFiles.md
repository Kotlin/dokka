---
layout: post
title: streamFiles
---
[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [AnalysisEnvironment](index.md) / [streamFiles](streamFiles.md)

# streamFiles
Streams files into [processor] and returns a stream of its results
```
public fun <T> streamFiles(processor: (BindingContext, JetFile)->T): Stream<T>
```
## Description
```
public fun <T> streamFiles(processor: (BindingContext, JetFile)->T): Stream<T>
```


**processor**
is a function to receive context for symbol resolution and file for processing

