plugins {
    id("io.vigier.java-library-conventions")
    id("maven-publish")
}

group = "io.vigier.cursorpaging-jpa"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
//    implementation("org.springframework.data:spring-data-commons")
//    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")

}

tasks.test {
    useJUnitPlatform()
}
val gitHubPkgUserName = project.findProperty("gitHubPkgUserName") as String?
val gitHubPkgPassword = project.findProperty("gitHubPkgPassword") as String?

publishing {
    repositories {
        maven {
            credentials {
                username = gitHubPkgUserName
                password = gitHubPkgPassword
            }
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/p3t/spring-cursorpaging")
        }
    }
    publications {
        create<MavenPublication>("spring-cursorpaging-jpa") {
            from(components["java"])
            groupId = "io.vigier.cursorpaging"
            artifactId = "spring-cursorpaging-jpa"
        }
    }
}