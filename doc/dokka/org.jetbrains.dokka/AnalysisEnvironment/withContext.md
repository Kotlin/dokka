---
layout: post
title: withContext
---
[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [AnalysisEnvironment](index.md) / [withContext](withContext.md)

# withContext
Executes [processor] when analysis is complete.
```
public fun <T> withContext(processor: (JetCoreEnvironment, ModuleDescriptor, BindingContext)->T): T
public fun <T> withContext(processor: (ModuleDescriptor, BindingContext)->T): T
```
## Description
```
public fun <T> withContext(processor: (JetCoreEnvironment, ModuleDescriptor, BindingContext)->T): T
```


**processor**
is a function to receive compiler environment, module and context for symbol resolution

```
public fun <T> withContext(processor: (ModuleDescriptor, BindingContext)->T): T
```


**processor**
is a function to receive compiler module and context for symbol resolution

