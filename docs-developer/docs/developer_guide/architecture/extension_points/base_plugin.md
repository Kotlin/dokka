# Base plugin

`DokkaBase` represents Dokka's _Base_ plugin, which provides a number of sensible default implementations for 
`CoreExtensions`, as well as declares its own, more high-level abstractions and extension points to be used from other 
plugins and output formats.

If you want to develop a simple plugin that only changes a few details, it is very convenient to rely on 
default implementations and use extension points defined in `DokkaBase`, as it reduces the scope of changes you need to make. 

`DokkaBase` is used extensively in Dokka's own output formats.

You can learn how to add, use, override and configure extensions and extension points in
[Introduction to Extensions](extension_points.md) - all of that information is applicable to the `DokkaBase` plugin as well.

## Extension points

Some notable extension points defined in Dokka's Base plugin.

### PreMergeDocumentableTransformer

`PreMergeDocumentableTransformer` is very similar to the 
[DocumentableTransformer](core_extension_points.md#documentabletransformer) core extension point, but it is used during 
an earlier stage by the [Single module generation](generation_implementations.md#singlemodulegeneration).

This extension point allows you to apply any transformations to the [Documentables model](../data_model/documentable_model.md) 
before the project's [source sets](https://kotlinlang.org/docs/multiplatform-discover-project.html#source-sets) are merged.

It is useful if you want to filter/map existing documentables. For example, if you want to exclude members annotated with
`@Internal`, you most likely need an implementation of `PreMergeDocumentableTransformer`.

For simple condition-based filtering of documentables, consider extending
`SuppressedByConditionDocumentableFilterTransformer` - it implements `PreMergeDocumentableTransformer` and only
requires one function to be overridden, whereas the rest is taken care of.
