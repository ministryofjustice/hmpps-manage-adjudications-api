import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "uk.gov.justice.digital.hmpps.gradle"
version = "1.0-SNAPSHOT"
description = "Custom gradle tasks"

plugins {
  kotlin("jvm") version "2.2.20"
}

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(21)
}