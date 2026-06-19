// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.Required

@DSL
fun dataSource(block: DataSourceConfiguration.() -> Unit): DataSource = TODO()

class DataSource

class DataSourceConfiguration {
    @Required var url: String? = null
}

fun test(condition: Boolean) {
    <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>dataSource {
        if (condition) {
            url = "localhost"
        }
    }<!>
}
