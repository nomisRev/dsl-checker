package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.Required

class Plain {
    @Required("name must be set")
    var name: String? = null
}

fun named(id: String, @DSL block: Plain.() -> Unit): Plain = Plain().apply(block)

fun box(): String {
    val result = named("a") { name = "OK" }.name
    return if (result == "OK") "OK" else "Fail: $result"
}
