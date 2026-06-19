package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL
import org.jetbrains.kotlin.compiler.plugin.template.Required

@DSL
fun dataSource(block: DataSourceConfiguration.() -> Unit): DataSource {
    DataSourceConfiguration().block()
    return DataSource()
}

class DataSource

class DataSourceConfiguration {
    @Required var url: String? = null
}

fun box(): String {
    val condition = true
    dataSource {
        if (condition) {
            url = "localhost"
        } else {
            url = "remotehost"
        }
    }
    return "OK"
}
