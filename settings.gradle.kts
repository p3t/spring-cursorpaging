pluginManagement {
//    val springBootPluginVersion: String by settings // use project property with version
    plugins {
        id("com.gradle.develocity") version ("3.17.2")
        id("com.google.protobuf") version "0.9.4"
//        id("org.springframework.boot") version "${springBootPluginVersion}"
    }
    resolutionStrategy {
    }
    repositories {
    }
}

plugins {
    id("com.gradle.develocity")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}



rootProject.name = "spring-cursorpaging"

include("cursorpaging-jpa", "cursorpaging-jpa-api")
include("cursorpaging-examples:webapp-with-maven")
//project("cursorpaging-example-webapp")
//    .projectDir = file("cursorpaging-examples/webapp-with-maven")

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}