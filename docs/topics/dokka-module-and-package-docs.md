[//]: # (title: Module documentation)

Documentation for a module as a whole, as well as packages in that module, can be provided as separate Markdown files.

## File format

Inside the Markdown file, the documentation for the module as a whole and for individual packages is introduced by the corresponding
first-level headings. The text of the heading **must** be **Module `<module name>`** for a module, and **Package `<package qualified name>`**
for a package. 

The file doesn't have to contain both module and package documentation. You can have files that contain only package or 
module documentation. You can even have a Markdown file per module or package.

Using [Markdown syntax](https://www.markdownguide.org/basic-syntax/), you can add:
* Headings up to level 6
* Emphasis with bold or italic formatting
* Links
* Inline code
* Code blocks
* Blockquotes

Here's an example file containing both module and package documentation:

```text
# Module kotlin-demo

This content appears under your module name.

# Package org.jetbrains.kotlin.demo

This content appears under your package name in the packages list.
It also appears under the first-level heading on your package's page.

## Level 2 heading for package org.jetbrains.kotlin.demo

Content after this heading is also part of documentation for org.jetbrains.kotlin.demo

# Package org.jetbrains.kotlin.demo2

This content appears under your package name in the packages list.
It also appears under the first-level heading on your package's page.

## Level 2 heading for package org.jetbrains.kotlin.demo

Content after this heading is also part of documentation for `org.jetbrains.kotlin.demo2`
```

To explore an example project with Gradle, see [Dokka gradle example](https://github.com/Kotlin/dokka/tree/master/examples/gradle/dokka-gradle-example).

## Pass files to Dokka

To pass these files to Dokka, you need to use the relevant **includes** option for Gradle, Maven, or CLI:

<tabs group="build-script">
<tab title="Gradle" group-key="gradle">

Use the [includes](dokka-gradle.md#includes) option in [Source set configuration](dokka-gradle.md#source-set-configuration).

</tab>

<tab title="Maven" group-key="mvn">

Use the [includes](dokka-maven.md#includes) option in [General configuration](dokka-maven.md#general-configuration).

</tab>

<tab title="CLI" group-key="cli">

If you are using command line configuration, use the [includes](dokka-cli.md#includes-cli) option in 
[Source set options](dokka-cli.md#source-set-options).

If you are using JSON configuration, use the [includes](dokka-cli.md#includes-json) option in 
[General configuration](dokka-cli.md#general-configuration).

</tab>
</tabs>
