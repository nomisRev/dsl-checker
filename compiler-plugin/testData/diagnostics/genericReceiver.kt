// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.OnlyInCombination
import org.jetbrains.kotlin.compiler.plugin.template.Required

class Generic<T> {
    @Required("name must be set")
    var name: String? = null

    @Required("value must be set")
    var value: T? = null
}

@DSL
fun <T> generic(block: Generic<T>.() -> Unit): Generic<T> = Generic<T>().apply(block)

@DSL
fun genericString(block: Generic<String>.() -> Unit): Generic<String> = Generic<String>().apply(block)

fun <T> named(id: String, @DSL block: Generic<T>.() -> Unit): Generic<T> = Generic<T>().apply(block)

class GenericAuth<T> {
    @OnlyInCombination(group = "auth", message = "user must be set with password")
    var user: T? = null

    @OnlyInCombination(group = "auth", message = "password must be set with user")
    var password: String? = null
}

@DSL
fun <T> auth(block: GenericAuth<T>.() -> Unit) {}

fun test() {
    generic<Int> {
        name = "a"
        value = 1
    }

    genericString {
        name = "a"
        value = "b"
    }

    named<Int>("a") {
        name = "a"
        value = 1
    }

    <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>generic<Int> {
        name = "a"
    }<!>

    <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>genericString { }<!>

    named<Int>("a") <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>{ value = 1 }<!>

    auth<Int> {
        user = 1
        password = "p"
    }

    <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>auth<Int> { user = 1 }<!>
}
