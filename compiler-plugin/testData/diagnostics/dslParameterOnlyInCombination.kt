// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.OnlyInCombination

class Auth {
    @OnlyInCombination(group = "auth", message = "user must be set with password")
    var user: String? = null

    @OnlyInCombination(group = "auth", message = "password must be set with user")
    var password: String? = null
}

fun auth(id: String, @DSL block: Auth.() -> Unit) {}

fun test() {
    auth("a") <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>{ user = "u" }<!>

    auth("a") {
        user = "u"
        password = "p"
    }

    auth("a") { }
}
