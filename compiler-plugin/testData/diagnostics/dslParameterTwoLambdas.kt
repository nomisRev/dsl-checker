// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.Required

class First {
    @Required("first name must be set")
    var name: String? = null
}

class Second {
    @Required("second name must be set")
    var name: String? = null
}

fun pair(@DSL first: First.() -> Unit, @DSL second: Second.() -> Unit, other: First.() -> Unit) {}

fun test() {
    pair({ name = "a" }, <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>{ }<!>) { }

    pair(<!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>{ }<!>, <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>{ }<!>) { }

    pair({ name = "a" }, { name = "b" }) { }
}
