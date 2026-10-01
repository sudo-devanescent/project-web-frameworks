plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.hostops"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
    // JPA se habilitará cuando configuremos la persistencia con PostgreSQL.
	// implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	// Seguridad se habilitará cuando exista la autenticación real (Spring Security).
	// implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	// implementation("org.thymeleaf.extras:thymeleaf-extras-springsecurity6")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    // Pruebas de JPA se habilitarán junto con la persistencia.
	// testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    // Las pruebas de seguridad se habilitarán con la autenticación real.
	// testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
