plugins {
    id("io.vigier.java-library-conventions")
    id("io.vigier.java-publish-conventions")
}

group = "io.vigier.cursorpaging"

ext["artifactId"] = findProperty("artifactId") ?: "cursorpaging-jpa"


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    api("jakarta.validation:jakarta.validation-api")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.retry:spring-retry")
    testAnnotationProcessor("org.hibernate:hibernate-jpamodelgen:6.6.1.Final")
    testRuntimeOnly("org.postgresql:postgresql")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}

