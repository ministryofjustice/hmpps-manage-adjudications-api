package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender

class OffenceCodeParagraphDescriptionsTest {

  @ParameterizedTest
  @CsvSource(
    "55:2,YOI_2_ADULT_1A",
    "55:1A,YOI_1_ADULT_1",
    "55:1L,YOI_1_ADULT_1",
    "55:1M,YOI_1_ADULT_1",
    "55:1E,YOI_1_ADULT_1",
    "55:5,YOI_5_ADULT_4",
    "55:6,YOI_6_ADULT_5",
    "55:8,YOI_8",
    "55:9D,YOI_9",
    "55:9E,YOI_9",
    "55:13,YOI_13_ADULT_12",
    "55:13A,YOI_13_ADULT_12",
    "55:15,YOI_15_ADULT_14",
    "55:14B,YOI_14_ADULT_13",
    "55:16,YOI_16",
    "55:27,YOI_27",
    "55:26B,YOI_26_ADULT_23",
    "55:26C,YOI_25_ADULT_22",
    "55:10,YOI_10",
    "55:13C,YOI_13_ADULT_12",
    "55:13B,YOI_13_ADULT_12",
    "55:11,YOI_11",
    "55:12,YOI_12",
    "55:17,YOI_17",
    "55:18,YOI_18",
    "55:19,YOI_19",
    "55:21B,YOI_21",
    "55:21C,YOI_21",
    "55:21A,YOI_21",
    "55:23,YOI_23_ADULT_20A",
    "55:20,YOI_22_ADULT_20",
    "55:25,YOI_25_ADULT_22",
    "55:26,YOI_26_ADULT_23",
    "55:3A,YOI_3_ADULT_2",
    "55:3C,YOI_3_ADULT_2",
    "55:3D,YOI_3_ADULT_2",
    "55:3B,YOI_3_ADULT_2",
    "55:4,YOI_4",
    "55:7,YOI_7",
    "55:20A,YOI_20",
    "55:20B,YOI_20",
    "55:24,YOI_24_ADULT_21",
    "55:28,YOI_28",
    "51:1A,YOI_2_ADULT_1A",
    "51:1B,YOI_1_ADULT_1",
    "51:1J,YOI_1_ADULT_1",
    "51:1N,YOI_1_ADULT_1",
    "51:1F,YOI_1_ADULT_1",
    "51:4,YOI_5_ADULT_4",
    "51:5,YOI_6_ADULT_5",
    "51:7,ADULT_7",
    "51:8D,ADULT_8",
    "51:8E,ADULT_8",
    "51:12,YOI_13_ADULT_12",
    "51:12A,YOI_13_ADULT_12",
    "51:14,YOI_15_ADULT_14",
    "51:13B,YOI_14_ADULT_13",
    "51:15,ADULT_15",
    "51:24,ADULT_24",
    "51:23AP,YOI_26_ADULT_23",
    "51:25Z,YOI_25_ADULT_22",
    "51:9,ADULT_9",
    "51:12AQ,YOI_13_ADULT_12",
    "51:10,ADULT_10",
    "51:11,ADULT_11",
    "51:16,ADULT_16",
    "51:17,ADULT_17",
    "51:19B,ADULT_19",
    "51:19C,ADULT_19",
    "51:19A,ADULT_19",
    "51:20A,YOI_23_ADULT_20A",
    "51:20,YOI_22_ADULT_20",
    "51:22,YOI_25_ADULT_22",
    "51:23,YOI_26_ADULT_23",
    "51:24A, ADULT_24A",
    "51:2A,YOI_3_ADULT_2",
    "51:2C,YOI_3_ADULT_2",
    "51:2D,YOI_3_ADULT_2",
    "51:2B,YOI_3_ADULT_2",
    "51:3,ADULT_3",
    "51:6,ADULT_6",
    "51:18A,ADULT_18",
    "51:18B,ADULT_18",
    "51:21,YOI_24_ADULT_21",
    "51:17A,ADULT_17A",
  )
  fun `get paragraph by offence code`(code: String, answer: Descriptions) {
    assert(OffenceCodes.values().first { it.getNomisCode() == code }.paragraphDescription == answer)
  }

  @ParameterizedTest
  @EnumSource(Descriptions::class)
  fun `ensure all descriptions have a mapping `(description: Descriptions) {
    if (description != Descriptions.DEFAULT) {
      assert(OffenceCodes.values().map { it.paragraphDescription }.contains(description))
    }
  }

  @ParameterizedTest
  @EnumSource(Gender::class)
  fun `pronouns test`(gender: Gender) {
    assert(
      OffenceCodes.ADULT_51_8D.paragraphDescription.getParagraphDescription(gender = gender).contains(
        gender.pronouns.first { it.type == PronounTypes.OBJECT_PERSONAL }.value,
      ),
    )
    assert(
      OffenceCodes.YOI_55_6.paragraphDescription.getParagraphDescription(gender = gender).contains(
        gender.pronouns.first { it.type == PronounTypes.POSSESSIVE }.value,
      ),
    )
    assert(
      OffenceCodes.YOI_55_26B.paragraphDescription.getParagraphDescription(gender = gender).contains(
        gender.pronouns.first { it.type == PronounTypes.SUBJECT_PERSONAL }.value,
      ),
    )
    assert(
      OffenceCodes.ADULT_51_18A.paragraphDescription.getParagraphDescription(gender = gender).contains(
        gender.pronouns.first { it.type == PronounTypes.REFLEXIVE }.value,
      ),
    )
  }
}
