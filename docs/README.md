# Dokka documentation

This folder contains the Dokka documentation that is available on [kotlinlang.org](https://kotlinlang.org/).

Our documentation is written in  Markdown format with some domain specific language (DSL) that is used at JetBrains.

## Project structure

This project contains:
* A `topics` directory,  which contains our documentation in Markdown format.
* A `dokka.tree` file, that describes the site navigation structure.
* A `vars.list` file, that contains a list of variables that you can use throughout documentation.

## DSL guide

This section explains what DSL you can use to create different document elements.

### Title

To declare the title of your document:

```text
[//]: # (title: Name of topic)
```

Every document must have a title. By default, this title is used in the side menu.

As the title is a level 1 header, it must be the only level 1 header in your document. All other headers within your document must be at least level 2, otherwise the side menu may not work as expected.

### Notes

To add a note:

```text
> This is a simple note
>
{type="note"}
```

### Tips

To add a tip:

```text
> This is a useful tip
>
{type="tip"}
```

### Warning

To add a warning:

```text
> This is a warning
>
{type="warning"}
```

### Tabs

Tabs can be used to save space in your document, allowing you to show different text in the same space depending on the tab chosen.

Content within tabs isn't limited to text. You can also add code blocks, tips, etc.

```text
<tabs group="build-script">

<tab title="Kotlin" group-key="kotlin">
Instructions specific to Kotlin
</tab>

<tab title="Groovy" group-key="groovy">
Instructions specific to Groovy
</tab>

<tab title="Maven" group-key="mvn">
Instructions specific to Maven
</tab>

<tab title="CLI" group-key="cli">
Instructions specific to CLI
</tab>

</tabs>
```

You can use the `group-key` parameter to link tabs in a document together. Grouping tabs like this means that if your reader selects a particular tab, e.g. "Groovy", then other tabbed sections in your document will also show the tab for "Groovy" first.

## Documentation preview

Unfortunately, at the moment, to properly preview documentation you need to be a JetBrains employee
or have access to internal infrastructure.

If you do have access, download `Stardust` plugin (ask around for instructions), and on the right sidebar you'll see
`Stardust Article Preview` tab, open it.
