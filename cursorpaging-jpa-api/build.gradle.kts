plugins {
	id("io.vigier.java-library-conventions")
	id("io.vigier.java-publish-conventions")
	id("com.google.protobuf")
}

group = "io.vigier.cursorpaging.jpa.api"
ext["artifactId"] = findProperty("artifactId") ?: "spring-cursorpaging-jpa-api"

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":cursorpaging-jpa"))
	implementation("com.google.protobuf:protobuf-java:4.26.1")
	implementation("org.springframework:spring-core")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("jakarta.validation:jakarta.validation-api")
	implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")

	testImplementation("org.mockito:mockito-core:5.12.0")
	testImplementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

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
		artifact = "com.google.protobuf:protoc:4.26.1"
	}
	generateProtoTasks {
		java{
		}
	}
}

sourceSets {
	main {
		proto {
			srcDir("src/main/proto")
		}
	}
}