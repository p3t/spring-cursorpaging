plugins {
    id("io.vigier.java-library-conventions")
    id("io.vigier.java-publish-conventions")
}

group = "io.vigier.cursorpaging"

ext["artifactId"] = findProperty("artifactId") ?: "cursorpaging-jpa"


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    api("jakarta.validation:jakarta.validation-api")
    api("jakarta.persistence:jakarta.persistence-api")
    api("org.springframework.data:spring-data-jpa")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.yaml:snakeyaml")


    testImplementation("org.hibernate.orm:hibernate-core")
    testImplementation("jakarta.transaction:jakarta.transaction-api")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.retry:spring-retry")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.assertj:assertj-core")
    testAnnotationProcessor("org.hibernate:hibernate-jpamodelgen:6.6.1.Final")
    testRuntimeOnly("org.postgresql:postgresql")
}

publishing {
    publications.named<MavenPublication>("mavenJava") {
        pom {
            description = "Cursor based paging support for Spring Data JPA repositories"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}

