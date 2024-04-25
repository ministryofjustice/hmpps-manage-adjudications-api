package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender

class OffenceCodeLookupServiceTest {
  private val offenceCodeLookupService: OffenceCodeLookupService = OffenceCodeLookupService()

  @Test
  fun `offence codes have values set for all items`() {
    assertValuesSetForAllItems(1001..1008)
    assertValuesSetForAllItems(1021..1022)
    assertValuesSetForAllItems(2001..2004)
    assertValuesSetForAllItems(2021..2021)
    assertValuesSetForAllItems(3001..3001)
    assertValuesSetForAllItems(4001..4001)
    assertValuesSetForAllItems(5001..5001)
    assertValuesSetForAllItems(6001..6001)
    assertValuesSetForAllItems(7001..7002)
    assertValuesSetForAllItems(8001..8002)
    assertValuesSetForAllItems(9001..9002)
    assertValuesSetForAllItems(10001..10001)
    assertValuesSetForAllItems(11001..11001)
    assertValuesSetForAllItems(12001..12002)
    assertValuesSetForAllItems(12101..12102)
    assertValuesSetForAllItems(13001..13001)
    assertValuesSetForAllItems(14001..14001)
    assertValuesSetForAllItems(15001..15001)
    assertValuesSetForAllItems(16001..16001)
    assertValuesSetForAllItems(17001..17002)
    assertValuesSetForAllItems(18001..18002)
    assertValuesSetForAllItems(19001..19003)
    assertValuesSetForAllItems(20001..20002)
    assertValuesSetForAllItems(21001..21001)
    assertValuesSetForAllItems(22001..22001)
    assertValuesSetForAllItems(23101..23101)
    assertValuesSetForAllItems(23001..23002)
    assertValuesSetForAllItems(23101..23101)
    assertValuesSetForAllItems(23201..23202)
    assertValuesSetForAllItems(24001..24002)
    assertValuesSetForAllItems(24101..24101)
  }

  @Test
  fun `get offence code for yoi`() {
    assertThat(offenceCodeLookupService.getOffenceCode(1002, true)).isEqualTo(OffenceCodes.YOI_55_1A)
  }

  @Test
  fun `get offence code for adult`() {
    assertThat(offenceCodeLookupService.getOffenceCode(1002, false)).isEqualTo(OffenceCodes.ADULT_51_1B)
  }

  @Test
  fun `ensure we do not make up duplicate offence codes when adding new offences`() {
    /*
     due to the nature of the offence code, ie someone tried to mirror the paragraph, then realised they change by yoi and adult,
     need to ensure the numbers we make up do not clash with existing ones.

     ie the offence code "magic" number, is used by both the YOI and Adult decision paths, for the same offence, but will have a different para title.
     The codes, such as 1001 are designed to represent para 1(a) for instance, but para 2 for yoi

     Ideally we would not have used such a system, but its now linked to the front end, api and database.
     */
    assertThat(OffenceCodes.entries.flatMap { it.uniqueOffenceCodes }.groupBy { it }.values.any { it.size > 2 }).isFalse
  }

  @Test
  fun `get version 1 codes`() {
    val adultOffences = offenceCodeLookupService.getAdultOffenceCodesByVersion(1)
    val youthOffences = offenceCodeLookupService.getYouthOffenceCodesByVersion(1)

    assertThat(
      youthOffences.none {
        listOf(
          OffenceCodes.YOI_55_19_24,
          OffenceCodes.YOI_55_23_24,
          OffenceCodes.YOI_55_28_24,
          OffenceCodes.YOI_55_26A_24,
          OffenceCodes.YOI_55_2_24,
          OffenceCodes.YOI_55_2A_24,
          OffenceCodes.YOI_55_2B_24,
          OffenceCodes.YOI_55_2C_24,
        ).contains(it)
      },
    ).isTrue

    assertThat(
      adultOffences.none {
        listOf(
          OffenceCodes.ADULT_51_17A_24,
          OffenceCodes.ADULT_51_1A_24,
          OffenceCodes.ADULT_51_1B_24,
          OffenceCodes.ADULT_51_1C_24,
          OffenceCodes.ADULT_51_1D_24,
          OffenceCodes.ADULT_51_20A_24,
          OffenceCodes.ADULT_51_23A_24,
          OffenceCodes.ADULT_51_24A_24,
        ).contains(it)
      },
    ).isTrue
  }

  @Test
  fun `get version 2 codes`() {
    val adultOffences = offenceCodeLookupService.getAdultOffenceCodesByVersion(2)
    val youthOffences = offenceCodeLookupService.getYouthOffenceCodesByVersion(2)

    assertThat(
      youthOffences.containsAll(
        listOf(
          OffenceCodes.YOI_55_19_24,
          OffenceCodes.YOI_55_23_24,
          OffenceCodes.YOI_55_28_24,
          OffenceCodes.YOI_55_26A_24,
          OffenceCodes.YOI_55_2_24,
          OffenceCodes.YOI_55_2A_24,
          OffenceCodes.YOI_55_2B_24,
          OffenceCodes.YOI_55_2C_24,
        ),
      ),
    ).isTrue

    assertThat(
      adultOffences.containsAll(
        listOf(
          OffenceCodes.ADULT_51_17A_24,
          OffenceCodes.ADULT_51_1A_24,
          OffenceCodes.ADULT_51_1B_24,
          OffenceCodes.ADULT_51_1C_24,
          OffenceCodes.ADULT_51_1D_24,
          OffenceCodes.ADULT_51_20A_24,
          OffenceCodes.ADULT_51_23A_24,
          OffenceCodes.ADULT_51_24A_24,
        ),
      ),
    ).isTrue

    assertThat(adultOffences.none { it.applicableVersions == listOf(1) }).isTrue
    assertThat(youthOffences.none { it.applicableVersions == listOf(1) }).isTrue
  }

  private fun assertValuesSetForAllItems(offenceCodes: IntRange) {
    offenceCodes.forEach {
      assertValuesSetForItem(it, false, "51:")
      assertValuesSetForItem(it, true, "55:")
    }
  }

  private fun assertValuesSetForItem(code: Int, isYouthOffender: Boolean, nomisCodePrefix: String) {
    val match = offenceCodeLookupService.getOffenceCode(code, isYouthOffender)
    if (match == OffenceCodes.MIGRATED_OFFENCE) return
    assertThat(match.paragraph).isNotBlank
    assertThat(match.paragraphDescription.getParagraphDescription(Gender.MALE)).isNotBlank
    assertThat(match.nomisCode).hasSizeGreaterThan(0)
    assertThat(match.nomisCode).startsWith(nomisCodePrefix)
    assertThat(match.getNomisCodeWithOthers()).startsWith(nomisCodePrefix)
  }
}
