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

rootProject.name = "spring-cursorpaging"

include("cursorpaging-jpa", "cursorpaging-testapp")
