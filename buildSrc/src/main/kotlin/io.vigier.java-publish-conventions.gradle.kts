import cl.franciscosolis.sonatypecentralupload.SonatypeCentralUploadTask

plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("cl.franciscosolis.sonatype-central-upload")
}
ext["artifactId"] = findProperty("artifactId") ?: "spring-cursorpaging-jpa"
ext["releaseVersion"] = findProperty("VERSION")  //
    ?: System.getenv("VERSION") //
            ?: file(rootDir.path + "/version.txt").readText().trim()
ext["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

publishing {
    var artifactId: String? by ext("")
    val propVersion = findProperty("VERSION")
    val envVersion = System.getenv("BUILD_VERSION")
    val fileVersion = file(rootDir.path + "/version.txt").readText().trim()
    val releaseVersion = propVersion ?: envVersion ?: fileVersion
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
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "io.vigier.cursorpaging"
            artifactId = artifactId.orEmpty()
        }
        withType<MavenPublication> {
            pom {
                version = releaseVersion.toString()
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
signing {
    val GPG_SIGNING_KEY_PASSWORD: String? by project
    val GPG_SIGNING_KEY: String? by project
    val GPG_KEY_ID: String? by project

    isRequired = true

    logger.info("Signing> Key ID: $GPG_KEY_ID")
    logger.info("Signing> Password is present: {}", GPG_SIGNING_KEY_PASSWORD.orEmpty().length)
    logger.info("Signing> Key is present: {}", GPG_SIGNING_KEY.orEmpty().length)

    useInMemoryPgpKeys(GPG_KEY_ID, GPG_SIGNING_KEY, GPG_SIGNING_KEY_PASSWORD)
    sign(publishing.publications["mavenJava"])
}

tasks.named<SonatypeCentralUploadTask>("sonatypeCentralUpload") {
    dependsOn("jar", "sourcesJar", "javadocJar", "generatePomFileForMavenJavaPublication")

    val mavenCentralUsername: String? by project
    val mavenCentralPassword: String? by project

    username = mavenCentralUsername ?: System.getenv("SONATYPE_CENTRAL_USERNAME")
    password = mavenCentralPassword ?: System.getenv("SONATYPE_CENTRAL_PASSWORD")

    // This is a list of files to upload. Ideally you would point to your jar file, source and javadoc jar (required by central)
    archives = files(
        tasks.named("jar"),
        tasks.named("sourcesJar"),
        tasks.named("javadocJar"),
//        tasks.named("signing"),
    )
    // This is the pom file to upload. This is required by central
    pom = file(
        layout.buildDirectory.file("publications/mavenJava/pom-default.xml")
//        tasks.publish.get().outputs.files.single()
//        tasks.named("generatePomFileForMavenJavaPublication").get().outputs.files.single()
    )

    val GPG_SIGNING_KEY_PASSWORD: String? by project
    val GPG_SIGNING_KEY: String? by project

    signingKey = (GPG_SIGNING_KEY ?: System.getenv("GPG_SIGNING_KEY"))
    signingKeyPassphrase = (GPG_SIGNING_KEY_PASSWORD ?: System.getenv("GPG_SIGNING_KEY_PASSWORD"))

    logger.info("Signing> Password is present: {}", signingKeyPassphrase.isPresent)
    logger.info("Signing> Key is present: {}", signingKey.isPresent)

    publishingType = "MANUAL"
}
