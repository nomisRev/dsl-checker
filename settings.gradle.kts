pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "dsl-checker"

include("compiler-plugin")
include("gradle-plugin")
include("plugin-annotations")
