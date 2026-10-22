// RUN_PIPELINE_TILL: BACKEND

package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.Required

class Plain {
    @Required("name must be set")
    var name: String? = null
}

fun named(id: String, @DSL block: Plain.() -> Unit = {}): Plain = Plain().apply(block)

fun withoutReceiver(id: String, @DSL block: (Plain) -> Unit) {}

fun test(configure: Plain.() -> Unit) {
    named("a", configure)

    named("a")

    withoutReceiver("a") { }
}
