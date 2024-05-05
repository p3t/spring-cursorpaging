plugins {
    id("maven-publish")
}

ext["artifactId"] = findProperty("artifactId") ?: "spring-cursorpaging-jpa"

publishing {
    var artifactId: String? by ext("")

    repositories {
        maven {
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
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
            artifactId = artifactId.orEmpty()
        }
        withType<MavenPublication> {
            pom {
                packaging = "jar"
                name.set("spring-cursorpaging-jpa")
                description.set("Spring cursor paging for JPA")
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