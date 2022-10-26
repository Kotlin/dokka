# docs

This module contains documentation for Dokka that is deployed to [kotlinlang.org](https://kotlinlang.org/)

Documentation format is basically markdown with some DSL that is used internally at JetBrains.

## Project structure

* `dokka.tree` represents Table of Contents
* `vars.list` contains variables that you can use throughout documentation
* `topics` directory contains documentation topics themselves

## DSL

### Title

Each page must have a title. By default, this title is used in ToC as well.

```text
[//]: # (title: Name of topic)
```

Note that title is basically a level 1 header, and it has to be the only one. So all other headers within topics must
be at least level 2, otherwise sidebar navigation may not work as expected.

### Notes

```text
> This is a simple note
>
{type="note"}
```

### Tips

```text
> This is a useful tip
>
{type="tip"}
```

### Warning

```text
> This is a warning
>
{type="warning"}
```

### Tabs

Tabs can be used to give instructions that are specific to a build system or something else.

Content inside tabs is not limited to just text - it can be code blocks, tips, etc.

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

Notice the use of `group-key` - this groups all tabs on the page, and when the user switches to a tab - it switches to
all tabs with this key throughout the whole page. This is convenient for the user, since if they switched to Groovy tab,
they probably want other tabs to be of that value as well.

## Documentation preview

Unfortunately, at the moment, to properly preview documentation you need to be a JetBrains employee
or have access to internal infrastructure.

If you do have access, download `Stardust` plugin (ask around for instructions), and on the right sidebar you'll see
`Stardust Article Preview` tab, open it.
