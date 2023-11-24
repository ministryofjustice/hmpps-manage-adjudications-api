package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ActivePrisonConfigTest {

  @Test
  fun `no env variable returns true`() {
    val activePrisonConfig = ActivePrisonConfig("")
    assertThat(activePrisonConfig.isActive("test")).isTrue
  }

  @Test
  fun `agency is available`() {
    val activePrisonConfig = ActivePrisonConfig("test")
    assertThat(activePrisonConfig.isActive("test")).isTrue
  }

  @Test
  fun `agency is not available`() {
    val activePrisonConfig = ActivePrisonConfig("nottest")
    assertThat(activePrisonConfig.isActive("test")).isFalse
  }
}
