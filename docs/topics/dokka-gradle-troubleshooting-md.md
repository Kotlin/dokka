[//]: # (title: Dokka Gradle troubleshooting)

This page describes common issues you may encounter when generating documentation with Dokka in a Gradle build.

If your issue is not listed here, report any feedback or problems in our [issue tracker](https://kotl.in/dokka-issues) 
or chat with the Dokka community in the official [Kotlin Slack](https://kotlinlang.slack.com/). Get a Slack invite [here](https://kotl.in/slack).

## Memory issues

In large projects, Dokka can consume a significant amount of memory to generate documentation.
This can exceed Gradle’s memory limits, especially when processing large volumes of data.

When Dokka generation runs out of memory, the build fails, 
and Gradle can throw exceptions like `java.lang.OutOfMemoryError: Metaspace`.

Active efforts are underway to improve Dokka's performance, although some limitations stem from Gradle.

If you encounter memory issues, try these workarounds:

* [Increasing heap space](#increase-heap-space)
* [Running Dokka within the Gradle process](#run-dokka-within-the-gradle-process)

### Increase heap space

One way to resolve memory issues is to increase the amount of Java heap memory for the Dokka generator process.
In the `build.gradle.kts` file, adjust the
following configuration option:

```kotlin
    dokka {
        // Dokka generates a new process managed by Gradle
        dokkaGeneratorIsolation = ProcessIsolation {
            // Configures heap size
            maxHeapSize = "4g"
        }
    }
```

In this example, the maximum heap size is set to 4 GB (`"4g"`). 
Adjust and test the value to find the optimal setting for your build.

If you find that Dokka requires a considerably expanded heap size, 
for example, significantly higher than Gradle's own memory usage,
[create an issue on Dokka's GitHub repository](https://kotl.in/dokka-issues).

> You have to apply this configuration to each subproject. 
> It is recommended that you configure Dokka in a convention
> plugin applied to all subprojects.
>
{style="note"}

### Run Dokka within the Gradle process

When both the Gradle build and Dokka generation require a lot of memory, they may run as separate processes,
consuming significant memory on a single machine.

To optimize memory usage, you can run Dokka within the same Gradle process instead of as a separate process. 
This allows you to configure the memory for Gradle once instead of allocating it separately for each process.

To run Dokka within the same Gradle process, adjust the following configuration option in the `build.gradle.kts` file:

```kotlin
    dokka {
        // Runs Dokka in the current Gradle process
        dokkaGeneratorIsolation = ClassLoaderIsolation()
    }
```

As with [increasing heap space](#increase-heap-space), test this configuration to confirm it works well for your project.

For more details on configuring Gradle's JVM memory, 
see the [Gradle documentation](https://docs.gradle.org/current/userguide/config_gradle.html#sec:configuring_jvm_memory).

> Changing the Java options for Gradle launches a new Gradle daemon, which may stay alive for a long time. You can [manually stop any other Gradle processes](https://docs.gradle.org/current/userguide/gradle_daemon.html#sec:stopping_an_existing_daemon).
>
> Additionally, Gradle issues with the `ClassLoaderIsolation()` configuration may [cause memory leaks](https://github.com/gradle/gradle/issues/18313).
>
{style="note"}