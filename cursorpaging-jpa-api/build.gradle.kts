plugins {
	id("io.vigier.java-library-conventions")
	id("io.vigier.java-publish-conventions")
	id("com.google.protobuf")
}

group = "io.vigier.cursorpaging"
ext["artifactId"] = findProperty("artifactId") ?: "cursorpaging-jpa-api"

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	api(project(":cursorpaging-jpa"))
	implementation("org.springframework:spring-core")
	api("jakarta.validation:jakarta.validation-api")

	implementation("com.fasterxml.jackson.core:jackson-annotations")
	implementation("com.fasterxml.jackson.core:jackson-core")
	implementation("com.fasterxml.jackson.core:jackson-databind")
//	implementation("com.fasterxml.jackson.core:jackson-datatype-jsr310")
	implementation("com.google.protobuf:protobuf-java:4.28.2")

	testImplementation("jakarta.persistence:jakarta.persistence-api")
	testImplementation("org.hibernate.orm:hibernate-jpamodelgen")
	testImplementation("org.assertj:assertj-core")
	testImplementation("org.hibernate.orm:hibernate-core")
	testImplementation("jakarta.transaction:jakarta.transaction-api")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.springframework.boot:spring-boot-test")
	testImplementation("org.springframework.retry:spring-retry")
	testImplementation("org.mockito:mockito-core")
	testImplementation("org.mockito:mockito-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:postgresql")

	testAnnotationProcessor("org.hibernate:hibernate-jpamodelgen:6.6.1.Final")
	testRuntimeOnly("org.postgresql:postgresql")
}
publishing {
	publications.named<MavenPublication>("mavenJava") {
		pom {
			description = "Serialization/Deserialization support of page-request for Spring-CursorPaging"
		}
	}
}
tasks {
	withType<Test> {
		useJUnitPlatform()
	}
	javadoc {
		options {
			(this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
		}
	}
	processResources {
		// On GitHub actions the proto file seems to be copied twice
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	}
}
protobuf {
	protoc {
		// Download from repositories
		artifact = "com.google.protobuf:protoc:4.28.2"
	}
	generateProtoTasks {
		java{
		}
	}
}

sourceSets {
	main {
		proto {
			srcDir("src/main/protobuf")
		}
	}
}