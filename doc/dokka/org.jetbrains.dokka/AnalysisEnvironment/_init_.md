---
layout: post
title: <init>
---
[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [AnalysisEnvironment](index.md) / [<init>](_init_.md)

# <init>
Kotlin as a service entry point
```
public AnalysisEnvironment(messageCollector: MessageCollector, body: AnalysisEnvironment.()->Unit)
```
## Description
```
public AnalysisEnvironment(messageCollector: MessageCollector, body: AnalysisEnvironment.()->Unit)
```
Configures environment, analyses files and provides facilities to perform code processing without emitting bytecode

**messageCollector**
is required by compiler infrastructure and will receive all compiler messages

**body**
is optional and can be used to configure environment without creating local variable

