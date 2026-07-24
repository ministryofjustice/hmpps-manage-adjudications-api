package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.Test

/**
 * Builds the schema so the SchemaSpy report can be generated against it.
 *
 * Excluded from normal test runs; run with `./gradlew -Pinit-db=true test` (see build.gradle.kts).
 * Starting the application context is enough - Flyway migrates on startup.
 */
class InitialiseDatabase : IntegrationTestBase() {

  @Test
  fun `initialises database`() {
    println("Database has been initialised by IntegrationTestBase")
  }
}
