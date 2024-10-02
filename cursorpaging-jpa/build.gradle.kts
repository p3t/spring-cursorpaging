plugins {
    id("io.vigier.java-library-conventions")
    id("io.vigier.java-publish-conventions")
    id("org.kordamp.gradle.coveralls")
}

group = "io.vigier.cursorpaging"

ext["artifactId"] = findProperty("artifactId") ?: "cursorpaging-jpa"


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    api("jakarta.validation:jakarta.validation-api")
//    implementation("org.springframework.data:spring-data-commons")
//    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

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

