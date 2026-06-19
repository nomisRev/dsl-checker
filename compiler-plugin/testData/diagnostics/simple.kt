// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.Required

@DSL
fun dataSource(block: DataSourceConfiguration.() -> Unit): DataSource = TODO()

class DataSource

class DataSourceConfiguration {
    @Required var url: String? = null
    @Required var username: String? = null
    @Required var password: String? = null
}

fun test() {
    <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>dataSource {
        url = "localhost"
        username = "postgresql"
    }<!>

    <!DSL_CALL_MISSING_REQUIRED_PROPERTIES!>dataSource {
        url = "localhost"
    }<!>

    dataSource {
        url = "localhost"
        username = "postgresql"
        password = "secret"
    }
}
