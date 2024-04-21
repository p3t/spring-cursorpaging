plugins {
	id("io.vigier.java-library-conventions")
	id("maven-publish")
	id("com.google.protobuf")
}

group = "io.vigier.cursorpaging.jpa.serial"

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
	testImplementation("org.mockito:mockito-core:5.11.0")
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
}
protobuf {
	protoc {
		// Download from repositories
		artifact = "com.google.protobuf:protoc:4.26.1"
	}
	generateProtoTasks {
//		all().configureEach { task ->
//			task.builtins {
//				java {
////					option "lite"
//				}
//			}
//		}
		java{

		}
	}
}