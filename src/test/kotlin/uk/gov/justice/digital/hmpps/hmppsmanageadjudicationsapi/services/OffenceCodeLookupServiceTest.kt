package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.SubmittedAdjudicationHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.Clock
import java.time.Instant.ofEpochMilli
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional
import javax.persistence.EntityNotFoundException

class OffenceCodeLookupServiceTest {
  private val offenceCodeLookupService: OffenceCodeLookupService = OffenceCodeLookupService()

  @Test
  fun `offence codes have values set for all items`() {
    assertValuesSetForAllItems(0)
    assertValuesSetForAllItems(1)
    assertValuesSetForAllItems(2)
    assertValuesSetForAllItems(3)
    assertValuesSetForAllItems(4)
    assertValuesSetForAllItems(5)
    assertValuesSetForAllItems(6)
    assertValuesSetForAllItems(7)
    assertValuesSetForAllItems(8)
    assertValuesSetForAllItems(9)

    assertValuesSetForAllItems(11)
    assertValuesSetForAllItems(12)
    assertValuesSetForAllItems(13)
    assertValuesSetForAllItems(14)

    assertValuesSetForAllItems(16)
    assertValuesSetForAllItems(17)
    assertValuesSetForAllItems(18)
    assertValuesSetForAllItems(19)
    assertValuesSetForAllItems(20)
    assertValuesSetForAllItems(21)
    assertValuesSetForAllItems(22)
    assertValuesSetForAllItems(23)
    assertValuesSetForAllItems(24)
    assertValuesSetForAllItems(25)
    assertValuesSetForAllItems(26)
    assertValuesSetForAllItems(27)
    assertValuesSetForAllItems(28)
    assertValuesSetForAllItems(29)
    assertValuesSetForAllItems(30)

    assertValuesSetForAllItems(32)
    assertValuesSetForAllItems(33)
    assertValuesSetForAllItems(34)
    assertValuesSetForAllItems(35)

    assertValuesSetForAllItems(37)
    assertValuesSetForAllItems(38)
    assertValuesSetForAllItems(39)
    assertValuesSetForAllItems(40)
    assertValuesSetForAllItems(41)

    assertValuesSetForAllItems(43)
    assertValuesSetForAllItems(44)

    assertValuesSetForAllItems(46)
    assertValuesSetForAllItems(47)
    assertValuesSetForAllItems(48)
    assertValuesSetForAllItems(49)

    assertValuesSetForAllItems(51)
    assertValuesSetForAllItems(52)
    assertValuesSetForAllItems(53)
    assertValuesSetForAllItems(54)

    assertValuesSetForAllItems(56)
    assertValuesSetForAllItems(57)
    assertValuesSetForAllItems(58)
  }

  fun assertValuesSetForAllItems(offenceCode: Int) {
    assertThat(offenceCodeLookupService.getParagraphNumber(offenceCode)).isNotBlank
    assertThat(offenceCodeLookupService.getParagraphDescription(offenceCode)).isNotBlank
    assertThat(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(offenceCode)).hasSizeGreaterThan(0)
    assertThat(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(offenceCode)).isNotBlank
  }
}
