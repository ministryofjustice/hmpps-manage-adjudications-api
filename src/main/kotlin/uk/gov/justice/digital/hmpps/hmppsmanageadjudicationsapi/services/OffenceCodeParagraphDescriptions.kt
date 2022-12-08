package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender

enum class Pronouns(val value: String, val type: PronounTypes) {
  HE("he", PronounTypes.OBJECT_PERSONAL),
  SHE("she", PronounTypes.OBJECT_PERSONAL),
  HIM("him", PronounTypes.SUBJECT_PERSONAL),
  HER("her", PronounTypes.SUBJECT_PERSONAL),
  HIS("his", PronounTypes.POSSESSIVE),
  POSSESSIVE_HER("her", PronounTypes.POSSESSIVE),
  HIMSELF("himself", PronounTypes.REFLEXIVE),
  HERSELF("herself", PronounTypes.REFLEXIVE);

  companion object {
    fun male() = listOf(HE, HIM, HIMSELF, HIS)
    fun female() = listOf(SHE, HER, POSSESSIVE_HER, HERSELF)
  }
}
enum class PronounTypes(val tag: String) {
  OBJECT_PERSONAL("<OBJ>"),
  SUBJECT_PERSONAL("<SUB>"),
  POSSESSIVE("<POS>"),
  REFLEXIVE("<REF>");
}

enum class Descriptions(val description: String) {
  ADULT_3("Denies access to any part of the prison to any officer or any person (other than a prisoner) who is at the prison for the purpose of working there"),
  ADULT_6("Intentionally obstructs an officer in the execution of his duty, or any person (other than a prisoner) who is at the prison for the purpose of working there, in the performance of his work"),
  ADULT_7("Escapes or absconds from prison or from legal custody"),
  ADULT_8("Fails to comply with any condition upon which ${PronounTypes.OBJECT_PERSONAL.tag} is temporarily released under rule 9"),
  ADULT_9("Is found with any substance in ${PronounTypes.POSSESSIVE.tag} urine which demonstrates that a controlled drug, pharmacy medication, prescription only medicine, psychoactive substance or specified substance has, whether in prison or while on temporary release under rule 9, been administered to ${PronounTypes.SUBJECT_PERSONAL.tag} by ${PronounTypes.REFLEXIVE.tag} or by another person (but subject to Rule 52)"),
  ADULT_10("Is intoxicated as a consequence of consuming any alcoholic beverage (but subject to rule 52A)"),
  ADULT_11("Consumes any alcoholic beverage whether or not provided to ${PronounTypes.SUBJECT_PERSONAL.tag} by another person (but subject to rule 52A)"),
  ADULT_15("Takes improperly any article belonging to another person or to a prison"),
  ADULT_16("Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not ${PronounTypes.POSSESSIVE.tag} own"),
  ADULT_17("Destroys or damages any part of a prison or any other property, other than ${PronounTypes.POSSESSIVE.tag} own"),
  ADULT_17A("Causes racially aggravated damage to, or destruction of, any part of a prison or any other property, other than ${PronounTypes.POSSESSIVE.tag} own"),
  ADULT_18("Absents ${PronounTypes.REFLEXIVE.tag} from any place ${PronounTypes.OBJECT_PERSONAL.tag} is required to be or is present at any place where ${PronounTypes.OBJECT_PERSONAL.tag} is not authorised to be"),
  ADULT_19("Is disrespectful to any officer, or any person (other than a prisoner) who is at the prison for the purpose of working there, or any person visiting a prison"),
  ADULT_24("Receives any controlled drug, pharmacy medicine, prescription only medicine, psychoactive substance or specified substance or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 38)"),
  YOI_4("Denies access to any part of the young offender institution to any officer or any person (other than an inmate) who is at the young offender institution for the purpose of working there"),
  YOI_7("Intentionally obstructs an officer in the execution of his duty, or any person (other than an inmate) who is at the young offender institution for the purpose of working there, in the performance of his work"),
  YOI_8("Escapes or absconds from a young offender institution or from legal custody"),
  YOI_9("Fails to comply with any condition upon which ${PronounTypes.OBJECT_PERSONAL.tag} was temporarily released under rule 5 of these rules"),
  YOI_10("Is found with any substance in ${PronounTypes.POSSESSIVE.tag} urine which demonstrates that a controlled drug, pharmacy medication, prescription only medicine, psychoactive substance or specified substance has, whether in prison or while on temporary release under rule 5, been administered to ${PronounTypes.SUBJECT_PERSONAL.tag} by ${PronounTypes.REFLEXIVE.tag} or by another person (but subject to Rule 56)"),
  YOI_11("Is intoxicated as a consequence of knowingly consuming any alcoholic beverage"),
  YOI_12("Knowingly consumes any alcoholic beverage, other than any provided to ${PronounTypes.SUBJECT_PERSONAL.tag} pursuant to a written order of the medical officer under rule 21(1)"),
  YOI_16("Takes improperly any article belonging to another person or to a young offender institution"),
  YOI_17("Intentionally or recklessly sets fire to any part of a young offender institution or any other property, whether or not ${PronounTypes.POSSESSIVE.tag} own"),
  YOI_18("Destroys or damages any part of a young offender institution or any other property other than ${PronounTypes.POSSESSIVE.tag} own"),
  YOI_19("Causes racially aggravated damage to, or destruction of, any part of a young offender institution or any other property, other than ${PronounTypes.POSSESSIVE.tag} own"),
  YOI_20("Absents ${PronounTypes.REFLEXIVE.tag} from any place where ${PronounTypes.OBJECT_PERSONAL.tag} is required to be or is present at any place where ${PronounTypes.OBJECT_PERSONAL.tag} is not authorised to be"),
  YOI_21(" Is disrespectful to any officer, or any person (other than an inmate) who is at the young offender institution for the purpose of working there, or any person visiting a young offender institution"),
  YOI_27("Receives any controlled drug, pharmacy medicine, prescription only medicine, psychoactive substance or specified substance or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 16)"),
  YOI_1_ADULT_1("Commits any assault"),
  YOI_2_ADULT_1A("Commits any racially aggravated assault"),
  YOI_3_ADULT_2("Detains any person against his will"),
  YOI_5_ADULT_4("Fights with any person"),
  YOI_6_ADULT_5("Intentionally endangers the health or personal safety of others or, by ${PronounTypes.POSSESSIVE.tag} conduct, is reckless whether such health or personal safety is endangered"),
  YOI_13_ADULT_12(
    "Has in ${PronounTypes.POSSESSIVE.tag} possession:<br>" +
      "<br>" +
      "(a) any unauthorised article, or<br>" +
      "<br>" +
      "(b) a greater quantity of any article than ${PronounTypes.OBJECT_PERSONAL.tag} is authorised to have"
  ),
  YOI_14_ADULT_13("Sells or delivers to any person any unauthorised article"),
  YOI_15_ADULT_14("Sells or, without permission, delivers to any person any article which ${PronounTypes.OBJECT_PERSONAL.tag} is allowed to have only for ${PronounTypes.POSSESSIVE.tag} own use"),
  YOI_22_ADULT_20("Uses threatening, abusive or insulting words or behaviour"),
  YOI_23_ADULT_20A("Uses threatening, abusive or insulting racist words or behaviour"),
  YOI_24_ADULT_21("Intentionally fails to work properly or, being required to work, refuses to do so"),
  YOI_25_ADULT_22("Disobeys any lawful order"),
  YOI_26_ADULT_23("Disobeys or fails to comply with any rule or regulation applying to ${PronounTypes.SUBJECT_PERSONAL.tag}"),
  DEFAULT("")
}

class OffenceCodeParagraphs {

  private val lookup = mapOf(
    "55:2" to Descriptions.YOI_2_ADULT_1A,
    "55:1A" to Descriptions.YOI_1_ADULT_1,
    "55:1L" to Descriptions.YOI_1_ADULT_1,
    "55:1M" to Descriptions.YOI_1_ADULT_1,
    "55:1E" to Descriptions.YOI_1_ADULT_1,
    "55:5" to Descriptions.YOI_5_ADULT_4,
    "55:6" to Descriptions.YOI_6_ADULT_5,
    "55:8" to Descriptions.YOI_8,
    "55:9D" to Descriptions.YOI_9,
    "55:9E" to Descriptions.YOI_9,
    "55:13" to Descriptions.YOI_13_ADULT_12,
    "55:13A" to Descriptions.YOI_13_ADULT_12,
    "55:15" to Descriptions.YOI_15_ADULT_14,
    "55:15" to Descriptions.YOI_15_ADULT_14,
    "55:14B" to Descriptions.YOI_14_ADULT_13,
    "55:16" to Descriptions.YOI_16,
    "55:17" to Descriptions.YOI_17,
    "55:27" to Descriptions.YOI_27,
    "55:26B" to Descriptions.YOI_26_ADULT_23,
    "55:26C" to Descriptions.YOI_26_ADULT_23,
    "55:10" to Descriptions.YOI_10,
    "55:13C" to Descriptions.YOI_13_ADULT_12,
    "55:13B" to Descriptions.YOI_13_ADULT_12,
    "55:11" to Descriptions.YOI_11,
    "55:12" to Descriptions.YOI_12,
    "55:18" to Descriptions.YOI_18,
    "55:19" to Descriptions.YOI_19,
    "55:21B" to Descriptions.YOI_21,
    "55:21C" to Descriptions.YOI_21,
    "55:21A" to Descriptions.YOI_21,
    "55:23" to Descriptions.YOI_23_ADULT_20A,
    "55:20" to Descriptions.YOI_22_ADULT_20,
    "55:25" to Descriptions.YOI_25_ADULT_22,
    "55:26" to Descriptions.YOI_26_ADULT_23,
    "55:3A" to Descriptions.YOI_3_ADULT_2,
    "55:3C" to Descriptions.YOI_3_ADULT_2,
    "55:3D" to Descriptions.YOI_3_ADULT_2,
    "55:3B" to Descriptions.YOI_3_ADULT_2,
    "55:4" to Descriptions.YOI_4,
    "55:7" to Descriptions.YOI_7,
    "55:20A" to Descriptions.YOI_20,
    "55:20B" to Descriptions.YOI_20,
    "55:24" to Descriptions.YOI_24_ADULT_21,
    "51:1A" to Descriptions.YOI_2_ADULT_1A,
    "51:1B" to Descriptions.YOI_1_ADULT_1,
    "51:1J" to Descriptions.YOI_1_ADULT_1,
    "51:1N" to Descriptions.YOI_1_ADULT_1,
    "51:1F" to Descriptions.YOI_1_ADULT_1,
    "51:4" to Descriptions.YOI_5_ADULT_4,
    "51:5" to Descriptions.YOI_6_ADULT_5,
    "51:7" to Descriptions.ADULT_7,
    "51:8D" to Descriptions.ADULT_8,
    "51:8E" to Descriptions.ADULT_8,
    "51:12" to Descriptions.YOI_13_ADULT_12,
    "51:12A" to Descriptions.YOI_13_ADULT_12,
    "51:14" to Descriptions.YOI_15_ADULT_14,
    "51:13B" to Descriptions.YOI_14_ADULT_13,
    "51:15" to Descriptions.ADULT_15,
    "51:24" to Descriptions.ADULT_24,
    "51:23AP" to Descriptions.YOI_26_ADULT_23,
    "51:25Z" to Descriptions.YOI_26_ADULT_23,
    "51:9" to Descriptions.ADULT_9,
    "51:12AQ" to Descriptions.YOI_13_ADULT_12,
    "51:10" to Descriptions.ADULT_10,
    "51:11" to Descriptions.ADULT_11,
    "51:16" to Descriptions.ADULT_16,
    "51:17A" to Descriptions.ADULT_17A,
    "51:17" to Descriptions.ADULT_17,
    "51:19B" to Descriptions.ADULT_19,
    "51:19C" to Descriptions.ADULT_19,
    "51:19A" to Descriptions.ADULT_19,
    "51:20" to Descriptions.YOI_22_ADULT_20,
    "51:20A" to Descriptions.YOI_23_ADULT_20A,
    "51:22" to Descriptions.YOI_25_ADULT_22,
    "51:23" to Descriptions.YOI_26_ADULT_23,
    "51:2A" to Descriptions.YOI_3_ADULT_2,
    "51:2B" to Descriptions.YOI_3_ADULT_2,
    "51:2C" to Descriptions.YOI_3_ADULT_2,
    "51:2D" to Descriptions.YOI_3_ADULT_2,
    "51:3" to Descriptions.ADULT_3,
    "51:6" to Descriptions.ADULT_6,
    "51:18A" to Descriptions.ADULT_18,
    "51:18B" to Descriptions.ADULT_18,
    "51:21" to Descriptions.YOI_24_ADULT_21
  )

  fun getParagraphDescription(nomisPrefixOffenceCode: String, gender: Gender): String {
    val description = lookup[nomisPrefixOffenceCode] ?: Descriptions.DEFAULT
    return formatGenderPronouns(
      description = description.description,
      gender = gender
    )
  }

  private fun formatGenderPronouns(description: String, gender: Gender): String {
    var result: String = description
    gender.pronouns.forEach {
      result = result.replace(it.type.tag, it.value)
    }
    return result
  }
}
