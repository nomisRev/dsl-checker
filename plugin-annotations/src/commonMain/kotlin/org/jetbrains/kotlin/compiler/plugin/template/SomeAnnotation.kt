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
public annotation class Required
