---
layout: api
title: withContext
---
[dokka](../../index.html) / [org.jetbrains.dokka](../index.html) / [AnalysisEnvironment](index.html) / [withContext](withContext.html)


# withContext

Executes [processor] when analysis is complete.
```
public fun <T> withContext(processor: (JetCoreEnvironment, ModuleDescriptor, BindingContext)->T): T
public fun <T> withContext(processor: (ModuleDescriptor, BindingContext)->T): T
```

# Description

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

