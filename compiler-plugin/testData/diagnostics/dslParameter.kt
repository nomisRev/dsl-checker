// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.Required

class Plain {
    @Required("name must be set")
    var name: String? = null
}

fun named(id: String, @DSL block: Plain.() -> Unit): Plain = Plain().apply(block)

fun test() {
    named("a") <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>{ }<!>

    named("a") { name = "x" }

    named(id = "a", block = <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>{ }<!>)

    named(block = { name = "x" }, id = "a")
}
