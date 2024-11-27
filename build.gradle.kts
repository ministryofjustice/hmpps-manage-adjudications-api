import uk.gov.justice.digital.hmpps.gradle.PortForwardRDSTask
import uk.gov.justice.digital.hmpps.gradle.PortForwardRedisTask
import uk.gov.justice.digital.hmpps.gradle.RevealSecretsTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.1.0"
  id("jacoco")
  kotlin("plugin.spring") version "2.1.0"
  kotlin("plugin.jpa") version "2.1.0"
  idea
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
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.1.0")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.apache.commons:commons-text:1.12.0")
  implementation("io.swagger:swagger-annotations:1.6.14")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0")

  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:4.5.0")
  implementation("io.opentelemetry:opentelemetry-api:1.44.1")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.10.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")
  runtimeOnly("com.h2database:h2:2.3.232")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:3.5.0")
  testImplementation("org.flywaydb:flyway-core")
  testImplementation("org.testcontainers:localstack:1.20.4")
  testImplementation("org.testcontainers:postgresql:1.20.4")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:1.44.1")
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
  register<PortForwardRDSTask>("portForwardRDS") {
    namespacePrefix = "hmpps-manage-adjudications-api"
  }

  register<PortForwardRedisTask>("portForwardRedis") {
    namespacePrefix = "hmpps-manage-adjudications-api"
  }

  register<RevealSecretsTask>("revealSecrets") {
    namespacePrefix = "hmpps-manage-adjudications-api"
  }

  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
  }
}
