pluginManagement {
//    val springBootPluginVersion: String by settings // use project property with version
    plugins {
//        id("org.springframework.boot") version "${springBootPluginVersion}"
    }
    resolutionStrategy {
    }
    repositories {
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "cursorpage"

include("lib", "testapp")
