// RUN_PIPELINE_TILL: FRONTEND

package foo.bar

import org.jetbrains.kotlin.compiler.plugin.template.DSL

@DSL
fun <!DSL_FUNCTION_WITH_PARAMETERS!>test<!>(message: String) {
}
