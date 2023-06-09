plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.2.0"
  id("jacoco")
  kotlin("plugin.spring") version "1.8.21"
  kotlin("plugin.jpa") version "1.8.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

repositories {
  maven { url = uri("https://repo.spring.io/milestone") }
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.apache.commons:commons-text:1.10.0")
  implementation("io.swagger:swagger-annotations:1.6.11")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.0.0")
  implementation("io.opentelemetry:opentelemetry-api:1.26.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.26.0")

  runtimeOnly("com.h2database:h2:2.1.214")
  runtimeOnly("org.flywaydb:flyway-core:9.19.1")
  runtimeOnly("org.postgresql:postgresql:42.6.0")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.11.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.38.0")
  testImplementation("org.flywaydb:flyway-core:9.19.1")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("org.testcontainers:localstack:1.18.3")
  testImplementation("org.testcontainers:postgresql:1.18.3")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.26.0")
}

allOpen {
  annotations(
    "jakarta.persistence.Entity",
    "jakarta.persistence.MappedSuperclass",
    "jakarta.persistence.Embeddable"
  )
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }
}
