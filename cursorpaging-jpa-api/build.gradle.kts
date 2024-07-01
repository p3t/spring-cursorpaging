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
	implementation("com.google.protobuf:protobuf-java:4.27.2")
	api("org.springframework:spring-core")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	api("jakarta.validation:jakarta.validation-api")
	api("jakarta.persistence:jakarta.persistence-api:3.2.0")

	testImplementation("org.mockito:mockito-core:5.12.0")
	testImplementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
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
		artifact = "com.google.protobuf:protoc:4.27.2"
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