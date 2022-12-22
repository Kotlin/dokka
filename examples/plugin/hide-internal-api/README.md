# Hide Internal API plugin example

This project represents a simple Dokka Plugin that was developed step-by-step in the
[Sample plugin](https://kotlin.github.io/dokka/1.7.20/developer_guide/plugin-development/sample-plugin-tutorial/)
tutorial. This is a frequent request with varying requirements.

The plugin excludes any declaration that is marked with `org.jetbrains.dokka.internal.test.Internal` annotation.
The annotation itself is not provided in this project and is instead matched by the fully qualified name only.
You can change it to your own internal annotation or to some other marker that suits you.

To learn how to install and debug it locally,
[see documentation](https://kotlin.github.io/dokka/1.7.20/developer_guide/plugin-development/sample-plugin-tutorial/#debugging).

___

Generally, you can use this project to get an idea of how to create Dokka plugins as it covers the basics of getting started.

This project was created from the [Dokka plugin template](https://github.com/Kotlin/dokka-plugin-template).
