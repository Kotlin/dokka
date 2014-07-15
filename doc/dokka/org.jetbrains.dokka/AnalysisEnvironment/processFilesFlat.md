---
layout: api
title: processFilesFlat
---
[dokka](../../index.html) / [org.jetbrains.dokka](../index.html) / [AnalysisEnvironment](index.html) / [processFilesFlat](processFilesFlat.html)


# processFilesFlat

Runs [processor] for each file and collects its results into single list

```
public fun <T> processFilesFlat(processor: (BindingContext, JetFile) -> List<T>): List<T>
```


### Description



**processor**
is a function to receive context for symbol resolution and file for processing

