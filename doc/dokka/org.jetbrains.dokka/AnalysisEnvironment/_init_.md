---
layout: api
title: <init>
---
[dokka](../../index.html) / [org.jetbrains.dokka](../index.html) / [AnalysisEnvironment](index.html) / [&lt;init&gt;](_init_.html)


# &lt;init&gt;

Kotlin as a service entry point

```
public AnalysisEnvironment(messageCollector: MessageCollector, body: AnalysisEnvironment.() -> Unit)
```


### Description

Configures environment, analyses files and provides facilities to perform code processing without emitting bytecode

**messageCollector**
is required by compiler infrastructure and will receive all compiler messages

**body**
is optional and can be used to configure environment without creating local variable

