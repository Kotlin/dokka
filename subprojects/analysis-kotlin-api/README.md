# Analysis: Kotlin API

Public API for interacting with Kotlin analysis, regardless of implementation. Contains no business logic.

Can be used to request additional information about Kotlin declarations.

Has to be used as a `compileOnly` dependency as Dokka bundles it by default in all runners.

The actual implementation (K1/K2/etc) will be resolved and bootstrapped during runtime, so the
user must not think about it.
