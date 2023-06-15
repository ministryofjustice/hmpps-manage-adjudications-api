package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ResourceServerConfiguration

class UserSecurityDetailsTest {

  @Test
  fun `username as the principal`() {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken("user", "pw")

    assertThat(UserDetails().currentUsername).isEqualTo("user")
  }

  @Test
  fun `user details as the principal`() {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(TestUserDetails("user"), "pw")

    assertThat(UserDetails().currentUsername).isEqualTo("user")
  }

  @Test
  fun `map as the principal`() {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(mapOf("username" to "user"), "pw")

    assertThat(UserDetails().currentUsername).isEqualTo("user")
  }

  @Test
  fun `principal is null`() {
    SecurityContextHolder.getContext().authentication = null

    assertThat(UserDetails().currentUsername).isEqualTo(null)
  }

  @Test
  fun `no active caseload for user throws validation exception `() {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(mapOf("username" to "user"), "pw")

    Assertions.assertThatThrownBy {
      UserDetails().activeCaseload
    }.isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("no active caseload set")
  }

  @Test
  fun `active case load is set `() {
    SecurityContextHolder.getContext().authentication = TestingAuthenticationToken(mapOf("username" to "user"), "pw").also {
      it.details = mapOf(ResourceServerConfiguration.ACTIVE_CASELOAD to "TEST")
    }

    assertThat(UserDetails().activeCaseload).isEqualTo("TEST")
  }
}
