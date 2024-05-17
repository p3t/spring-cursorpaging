plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal() // so that external plugins can be resolved in dependencies section
}

dependencies {
    val kordampVersion: String by extra("0.54.0")
//    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.0.9")
    implementation("io.freefair.gradle:lombok-plugin:8.6")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.2.5")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.5")
//    implementation("org.kordamp.gradle:plugin-gradle-plugin:$kordampVersion")
    implementation("org.kordamp.gradle:project-gradle-plugin:$kordampVersion")
    implementation("org.kordamp.gradle:spotbugs-gradle-plugin:$kordampVersion")
    implementation("org.kordamp.gradle:coveralls-gradle-plugin:$kordampVersion")
//    implementation("org.kordamp.gradle:base-gradle-plugin:$kordampVersion")
//    implementation("org.kordamp.gradle:jacoco-gradle-plugin:$kordampVersion")
    implementation("gradle.plugin.org.kt3k.gradle.plugin:coveralls-gradle-plugin:2.12.2")
    implementation("org.jreleaser:jreleaser-gradle-plugin:1.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
}

