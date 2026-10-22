package org.jetbrains.kotlin.compiler.plugin.template

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY

@Target(FUNCTION, CLASS)
@Retention(BINARY)
public annotation class DSL

@Target(PROPERTY)
@Retention(BINARY)
public annotation class Required(
    /** Custom message reported when this required property is missing from a DSL block. */
    val message: String = "",
)

@Target(PROPERTY)
@Retention(BINARY)
public annotation class OnlyInCombination(
    /** Name used to identify the group of properties that this property belongs to. */
    val group: String,
    /** Custom message reported when this required property is missing from a DSL block. */
    val message: String = "",
)

@Target(PROPERTY)
@Retention(BINARY)
public annotation class ExactlyOneOf(
    /** Name used to identify the group of properties that this property belongs to. */
    val group: String,
    /** Name used for this property when a DSL block sets none or more than one property of its group. */
    val message: String = "",
)
