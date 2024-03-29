plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.15.5"
  id("jacoco")
  kotlin("plugin.spring") version "1.9.23"
  kotlin("plugin.jpa") version "1.9.23"
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
  implementation("org.apache.commons:commons-text:1.11.0")
  implementation("io.swagger:swagger-annotations:1.6.14")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.4.0")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:3.1.1")
  implementation("io.opentelemetry:opentelemetry-api:1.36.0")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.2.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.0")
  runtimeOnly("com.h2database:h2:2.2.224")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.5")
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.2.7")
  testImplementation("org.flywaydb:flyway-core")
  testImplementation("org.testcontainers:localstack:1.19.7")
  testImplementation("org.testcontainers:postgresql:1.19.7")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.36.0")
}

allOpen {
  annotations(
    "jakarta.persistence.Entity",
    "jakarta.persistence.MappedSuperclass",
    "jakarta.persistence.Embeddable",
  )
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
