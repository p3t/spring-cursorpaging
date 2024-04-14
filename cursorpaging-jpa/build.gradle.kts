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

val GITHUBPKG_USERNAME: String by project
val GITHUBPKG_PASSWORD: String by project

println(GITHUBPKG_PASSWORD)
publishing {
    repositories {
        maven {
            credentials {
                username = GITHUBPKG_USERNAME
                password = GITHUBPKG_PASSWORD
            }
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/p3t/spring-cursorpaging")
        }
        maven {
            name = "staging"
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
    publications {
        create<MavenPublication>("spring-cursorpaging-jpa") {
            from(components["java"])
            groupId = "io.vigier.cursorpaging"
            artifactId = "spring-cursorpaging-jpa"
        }
        withType<MavenPublication> {
            pom {
                packaging = "jar"
                name.set("spring-cursorpaging-jpa")
                description.set("Spring Cursor Paging JPA")
                url.set("https://github.com/p3t/spring-cursorpaging/")
                inceptionYear.set("2024")
                licenses {
                    license {
                        name.set("Apache2 license")
                        url.set("https://apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("p3t")
                        name.set("Peter Vigier")
                        email.set("3204560+p3t@users.noreply.github.com")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:p3t/spring-cursorpaging.git")
                    developerConnection.set("scm:git:ssh:git@github.com:p3t/spring-cursorpaging.git")
                    url.set("https://github.com/p3t/spring-cursorpaging")
                }
            }
        }
    }
}