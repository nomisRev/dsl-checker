# Kotlin DSL Checker Compiler Plugin

This repository contains a Kotlin compiler plugin that performs **compile-time validation for DSL builders**.

The plugin introduces two runtime annotations:

- `@DSL` marks a DSL entry function.
- `@Required` marks builder properties that must be assigned inside the DSL lambda.
  - You can customize the compiler error text with `@Required(message = "...")`.

Example:

```kotlin
@DSL
fun dataSource(block: DataSourceConfiguration.() -> Unit): DataSource = TODO("Impl not relevant")

class DataSourceConfiguration {
    @Required(message = "This property is required to connect to the database")
    var url: String? = null
    @Required var username: String? = null
    @Required var password: String? = null
}
```

If a DSL block does not initialize all required fields, compilation fails:

```kotlin
val dataSourceA = dataSource {
    url = "localhost"
    username = "postgresql"
    // error: missing password
}

val dataSourceB = dataSource {
    url = "localhost"
    // error: missing username and password
}
```

## Modules

This project has three modules:

- [`:compiler-plugin`](compiler-plugin/src) — the compiler plugin itself.
- [`:plugin-annotations`](plugin-annotations/src/commonMain/kotlin) — the runtime annotations used by user code.
- [`:gradle-plugin`](gradle-plugin/src) — a small Gradle plugin that adds both the compiler plugin and the annotations dependency to a Kotlin project.

## How it works

The implementation is based on the Kotlin **K2/FIR frontend**:

- `SimplePluginRegistrar` registers the FIR extension.
- `SimpleAdditionalCheckersExtension` contributes a custom FIR function-call checker.
- The checker detects calls to `@DSL` functions, resolves the receiver type of the lambda, collects `@Required` properties from the builder class, and reports a compilation error when any required properties are missing.

This means the project focuses on **static DSL validation**, not code generation.

## Tests

The Kotlin compiler test framework is set up for this project.

- Add diagnostics tests under `compiler-plugin/testData/diagnostics`.
- Add box/codegen tests under `compiler-plugin/testData/box`.
- The generated JUnit 5 test classes are updated automatically when tests run, or manually with the `generateTests` Gradle task.

The current tests cover:

- successful DSL usage
- missing required DSL properties
- ordinary box tests for plugin wiring

To aid development, the Kotlin Compiler DevKit IntelliJ plugin is recommended and already configured for this repository.

[//]: # (Links)

[test-framework]: https://github.com/JetBrains/kotlin/blob/master/compiler/test-infrastructure/ReadMe.md
[test-plugin]: https://github.com/JetBrains/kotlin-compiler-devkit
