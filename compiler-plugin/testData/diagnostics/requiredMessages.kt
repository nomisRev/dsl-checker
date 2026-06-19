// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.Required

@DSL
fun dataSource(block: DataSourceConfiguration.() -> Unit): DataSource = TODO()

class DataSource

class DataSourceConfiguration {
    @Required(message = "URL is required")
    var url: String? = null

    @Required(message = "Username is required")
    var username: String? = null
}

fun test() {
    <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>dataSource {
    }<!>
}
