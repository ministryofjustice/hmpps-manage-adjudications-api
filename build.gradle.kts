plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.1.5-beta-3"
  kotlin("plugin.spring") version "1.6.20"
  kotlin("plugin.jpa") version "1.6.20"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("io.springfox:springfox-boot-starter:3.0.0")
  implementation("org.apache.commons:commons-text:1.9")

  runtimeOnly("com.h2database:h2:2.1.210")
  runtimeOnly("org.flywaydb:flyway-core:8.4.1")
  runtimeOnly("org.postgresql:postgresql:42.3.3")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("com.github.tomakehurst:wiremock-standalone:2.27.2")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.28.0")
  testImplementation("org.flywaydb:flyway-core:8.4.1")
}

allOpen {
  annotations(
    "javax.persistence.Entity",
    "javax.persistence.MappedSuperclass",
    "javax.persistence.Embeddable"
  )
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
}
