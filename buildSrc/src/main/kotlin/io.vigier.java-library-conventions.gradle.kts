// File: buildSrc/src/main/kotlin/java-project-conventions.gradle.kts
plugins {
    id("java-library")
    `jvm-test-suite`
    id("io.freefair.lombok")
    id("com.github.spotbugs")
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

group = "io.vigier.cursorpage"
version = "0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.4")
    }
}
tasks.named<Jar>("bootJar") {
    enabled = false
}
spotbugs {
    ignoreFailures = true
}

extra["hibernate.version"] = "6.4.4.Final"


dependencies {
    val lombokVersion: String by extra("1.18.30")
    val junitVersion: String by extra("5.10.2")
    val assertjVersion: String by extra("3.25.3")
    // Load BOM for Spring Boot.
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.3"))

    compileOnly("org.projectlombok:lombok:${lombokVersion}")
    compileOnly("org.hibernate:hibernate-jpamodelgen:6.4.4.Final")

    annotationProcessor("org.projectlombok:lombok:${lombokVersion}")

    // Testing
    testImplementation("org.assertj:assertj-core:${assertjVersion}")
}

java {
    version = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}
//jpaModelgen {
//    library = "org.hibernate:hibernate-jpamodelgen:6.4.4.Final"
//    jpaModelgenSourcesDir = "build/generated/sources/jpaModelgen/java"
//}
configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}