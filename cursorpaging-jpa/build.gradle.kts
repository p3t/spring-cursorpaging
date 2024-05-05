plugins {
    id("io.vigier.java-library-conventions")
    id("io.vigier.java-publish-conventions")
    id("maven-publish")
}

group = "io.vigier.cursorpaging-jpa"

ext["artifactId"] = findProperty("artifactId") ?: "spring-cursorpaging-jpa"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("jakarta.validation:jakarta.validation-api")
//    implementation("org.springframework.data:spring-data-commons")
//    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")

}

tasks.test {
    useJUnitPlatform()
}

