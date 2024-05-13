// File: buildSrc/src/main/kotlin/java-project-conventions.gradle.kts
plugins {
    id("java-library")
    `jvm-test-suite`
    id("io.freefair.lombok")
    id("com.github.spotbugs")
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

group = "io.vigier.cursorpaging"
version = "0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.2.5")
    }

}
tasks.named<Jar>("bootJar") {
    enabled = false
}
tasks {
    delombok {
        enabled = false
    }
    jar {
        archiveClassifier.set("")
    }
}
spotbugs {
    ignoreFailures = true
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
    version = JavaVersion.VERSION_17
    withSourcesJar()
    withJavadocJar()
}


//configurations {
//    compileOnly {
//        extendsFrom(configurations.annotationProcessor.get())
//    }
//}
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}