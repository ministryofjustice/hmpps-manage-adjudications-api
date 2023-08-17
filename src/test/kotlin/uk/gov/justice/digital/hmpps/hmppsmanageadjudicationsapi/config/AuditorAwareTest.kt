package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade

class AuditorAwareTest {
  private val authenticationFacade: AuthenticationFacade = mock()
  private val auditorAware = AuditorAware(authenticationFacade)

  @Test
  fun `substrings name correctly`() {
    whenever(authenticationFacade.currentUsername).thenReturn("hmpps-prisoner-from-nomis-migration-adjudications-1")
    assertThat(auditorAware.currentAuditor.get().length).isEqualTo(32)
  }

  @Test
  fun `just returns the username`() {
    whenever(authenticationFacade.currentUsername).thenReturn("dave")
    assertThat(auditorAware.currentAuditor.get()).isEqualTo("dave")
  }

  @Test
  fun `returns empty`() {
    whenever(authenticationFacade.currentUsername).thenReturn(null)
    assertThat(auditorAware.currentAuditor.isEmpty).isTrue
  }
}
