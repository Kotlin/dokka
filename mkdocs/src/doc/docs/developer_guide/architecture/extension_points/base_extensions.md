# Base extensions

`DokkaBase` class is a base plugin which defines a number of default implementations for `CoreExtensions` as well as
declares its own, more high-level extension points to be used from other plugins and output formats.

It's very convenient to use extension points and defaults defined in `DokkaBase` if you have an idea for a simple
plugin that only needs to provide a few extensions or change a single extension point and have everything else be the
default.

`DokkaBase` is used extensively for Dokka's own output formats such as `HTML`, `Markdown`, `Mathjax` and others.

You can learn how to add/use/override/configure extensions and extension points in
[Introduction to Extensions](introduction.md), all the information is applicable to `DokkaBase` plugin as well.
