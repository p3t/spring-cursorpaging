// Gradle build to include the example in the project build
// While the maven pom.xml es executed separately

plugins {
    java
    id("org.springframework.boot")
    id("io.vigier.java-library-conventions")
}

group = "io.vigier.cursorpaging.examples.webapp"
version = findProperty("version") ?: System.getenv("BUILD_VERSION") ?: "0-SNAPSHOT"

dependencies {
    val mapstructVersion: String by extra("1.5.5.Final")

    implementation(project(":cursorpaging-jpa"))
    implementation(project(":cursorpaging-jpa-api"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.hateoas:spring-hateoas")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-aspects")
    implementation("org.apache.commons:commons-lang3")
//    compileOnly("javax.xml.bind:jaxb-api")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.mapstruct:mapstruct:${mapstructVersion}")

    annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")
    annotationProcessor("org.hibernate:hibernate-jpamodelgen:6.6.1.Final")

//    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
//    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
//    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
//    implementation("org.liquibase:liquibase-core")


    runtimeOnly("org.postgresql:postgresql")
//    runtimeOnly("org.postgresql:r2dbc-postgresql")

//    annotationProcessor("org.hibernate:hibernate-jpamodelgen")
//    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
//    testImplementation("io.projectreactor:reactor-test")
//    testImplementation("org.testcontainers:mongodb")
//    testImplementation("org.testcontainers:r2dbc")
}

tasks {
    javadoc {
        options {
            (this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
        }
    }
    test {
        useJUnitPlatform()
    }

}
