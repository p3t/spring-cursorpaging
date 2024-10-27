import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

// File: buildSrc/src/main/kotlin/java-project-conventions.gradle.kts
plugins {
    id("java-library")
    `jvm-test-suite`
    id("io.freefair.lombok")
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
    `jacoco`
    id("org.kordamp.gradle.spotbugs")
    id("com.github.kt3k.coveralls")
    id("org.kordamp.gradle.coveralls")
    id("org.sonarqube")
}

group = "io.vigier.cursorpaging"
version = findProperty("version") ?: System.getenv("BUILD_VERSION") ?: "0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.5")
    }

}
// This is needed for sontarTypeCentralUpload! Has conflict with kordamp.java-project
java {
    withSourcesJar()
    withJavadocJar()
}
tasks.named<Jar>("bootJar") {
    enabled = false
}
sonar {
    properties {
        property("sonar.projectKey", "p3t_spring-cursorpaging")
        property("sonar.organization", "p3t")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
tasks {
    jacocoTestReport {
        dependsOn(tasks.test) // tests are required to run before generating the report
        reports {
            xml.required = true
            html.required = true
        }
    }
    delombok {
        enabled = false
    }
    jar {
        archiveClassifier.set("") // needed to remove "-plain" when bootJar = false
    }
    test {
        finalizedBy(tasks.jacocoTestReport)
        testLogging {
            events(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT)
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }
}

config {
    info {
        inceptionYear = "2024"
        vendor = "p3t/viger"
        tags = listOf("paging", "spring", "jpa", "cursor")
        description = "Cursor based paging support for Spring Data JPA"

        links {
            scm = "https://github.com/p3t/spring-cursorpaging.git"
        }

        people {
            person {
                id = "p3t"
                name = "P. Vigier"
                roles = listOf("developer", "maintainer")
            }
        }

        organization {
            name = "p3t"
            url = "https://github.com/p3t/spring-cursorpaging"
        }
    }
    licensing {
        enabled = false
        licenses {
            license {
                id = "Apache-2.0"
                primary = true
            }
        }
    }
    coverage {
        coveralls {
            enabled = false
        }
    }
    quality {
        spotbugs {
            enabled = true
            ignoreFailures = true
        }
    }
}

ext["hibernate.version"] = "6.4.4.Final"
ext["junitVersion"] = findProperty("junitVersion") ?: "5.10.2"

dependencies {
    val lombokVersion: String by extra("1.18.32")
    val junitVersion: String by extra("5.10.2")
    val assertjVersion: String by extra("3.25.3")


    // Load BOM for Spring Boot.
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.5"))
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    testCompileOnly("org.projectlombok:lombok:${lombokVersion}")

    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")
    testAnnotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    // Testing
    testImplementation("org.assertj:assertj-core:${assertjVersion}")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}