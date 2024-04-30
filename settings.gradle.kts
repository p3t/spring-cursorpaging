pluginManagement {
//    val springBootPluginVersion: String by settings // use project property with version
    plugins {
        id("com.google.protobuf") version "0.9.4"
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

include("cursorpaging-jpa", "cursorpaging-jpa-serializer", "cursorpaging-testapp")
