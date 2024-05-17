plugins {
    id("io.vigier.java-library-conventions")
    id("io.vigier.java-publish-conventions")
    id("org.kordamp.gradle.coveralls")
}

group = "io.vigier.cursorpaging-jpa"
version = "0-SNAPSHOT"

ext["artifactId"] = findProperty("artifactId") ?: "spring-cursorpaging-jpa"


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    api("jakarta.validation:jakarta.validation-api")
//    implementation("org.springframework.data:spring-data-commons")
//    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")

}

tasks.test {
    useJUnitPlatform()
}

