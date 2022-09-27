## Dokka custom format example

This example demonstrates a few things:

1. How to override css styles and add custom images.
2. How to change logo used in the header.
3. How to register a custom `Dokka` task with its own independent configuration.

### Running

`dokkaCustomFormat` task has been created in the buildscript of this example project with a few configuration changes.

In order to see the full effect of these changes, run `dokkaCustomFormat` task from your IDE or execute
the following command:

```bash
./gradlew clean dokkaCustomFormat
```

---

If you run any other `Dokka` task, such as `dokkaHtml`, you'll see vanilla `Dokka` without any alterations.
This is because changes to configuration are applied only within `dokkaCustomFormat` task. This can be useful
if you want to generate multiple versions of documentation with different configuration settings.
