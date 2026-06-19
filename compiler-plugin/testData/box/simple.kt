package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL

@DSL
fun box(): String {
    val result = "Hello world"
    return if (result == "Hello world") { "OK" } else { "Fail: $result" }
}
