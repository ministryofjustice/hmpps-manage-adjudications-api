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
  HERSELF("herself", PronounTypes.REFLEXIVE),
  ;

  companion object {
    fun male() = listOf(HE, HIM, HIMSELF, HIS)
    fun female() = listOf(SHE, HER, POSSESSIVE_HER, HERSELF)
  }
}
enum class PronounTypes(val tag: String) {
  OBJECT_PERSONAL("<OBJ>"),
  SUBJECT_PERSONAL("<SUB>"),
  POSSESSIVE("<POS>"),
  REFLEXIVE("<REF>"),
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
  ADULT_17A_24("Causes damage to, or destruction of, any part of a prison or any other property, other than ${PronounTypes.POSSESSIVE.tag} own, aggravated by a protected characteristic"),
  ADULT_18("Absents ${PronounTypes.REFLEXIVE.tag} from any place ${PronounTypes.OBJECT_PERSONAL.tag} is required to be or is present at any place where ${PronounTypes.OBJECT_PERSONAL.tag} is not authorised to be"),
  ADULT_19("Is disrespectful to any officer, or any person (other than a prisoner) who is at the prison for the purpose of working there, or any person visiting a prison"),
  ADULT_24("Receives any controlled drug, pharmacy medicine, prescription only medicine, psychoactive substance or specified substance or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 38)"),
  ADULT_24A("Displays, attaches or draws on any part of a prison, or on any other property, threatening, abusive or insulting racist words, drawings, symbols or other material"),
  ADULT_24A_24("Displays, attaches or draws on any part of a prison, or on any other property, threatening, abusive or insulting words, drawings, symbols or other material, which demonstrate, or are motivated (wholly or partly) by, hostility to persons based on them sharing a protected characteristic"),
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
  YOI_19_24("Causes damage to, or destruction of, any part of a young offender institution or any other property, other than ${PronounTypes.POSSESSIVE.tag} own, aggravated by a protected characteristic"),
  YOI_20("Absents ${PronounTypes.REFLEXIVE.tag} from any place where ${PronounTypes.OBJECT_PERSONAL.tag} is required to be or is present at any place where ${PronounTypes.OBJECT_PERSONAL.tag} is not authorised to be"),
  YOI_21(" Is disrespectful to any officer, or any person (other than an inmate) who is at the young offender institution for the purpose of working there, or any person visiting a young offender institution"),
  YOI_27("Receives any controlled drug, pharmacy medicine, prescription only medicine, psychoactive substance or specified substance or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 16)"),
  YOI_28("Displays, attaches or draws on any part of a young offender institution, or on any other property, threatening, abusive, or insulting racist words, drawings, symbols or other material"),
  YOI_28_24("Displays, attaches or draws on any part of a young offender institution, or on any other property, threatening, abusive or insulting words, drawings, symbols or other material, which demonstrate, or are motivated (wholly or partly) by, hostility to persons based on them sharing a protected characteristic"),
  YOI_1_ADULT_1("Commits any assault"),
  YOI_2A_24_ADULT_1B_24("Commits any sexual assault"),
  YOI_2B_24_ADULT_1C_24("Exposes ${PronounTypes.REFLEXIVE.tag}, or commits any other indecent or obscene act"),
  YOI_2C_24_ADULT_1D_24("Sexually harasses any person"),
  YOI_2_ADULT_1A("Commits any racially aggravated assault"),
  YOI_2A_24_ADULT_1A_24("Commits any assault aggravated by a protected characteristic"),
  YOI_3_ADULT_2("Detains any person against his will"),
  YOI_5_ADULT_4("Fights with any person"),
  YOI_6_ADULT_5("Intentionally endangers the health or personal safety of others or, by ${PronounTypes.POSSESSIVE.tag} conduct, is reckless whether such health or personal safety is endangered"),
  YOI_13_ADULT_12(
    "Has in ${PronounTypes.POSSESSIVE.tag} possession:<br>" +
      "<br>" +
      "(a) any unauthorised article, or<br>" +
      "<br>" +
      "(b) a greater quantity of any article than ${PronounTypes.OBJECT_PERSONAL.tag} is authorised to have",
  ),
  YOI_14_ADULT_13("Sells or delivers to any person any unauthorised article"),
  YOI_15_ADULT_14("Sells or, without permission, delivers to any person any article which ${PronounTypes.OBJECT_PERSONAL.tag} is allowed to have only for ${PronounTypes.POSSESSIVE.tag} own use"),
  YOI_22_ADULT_20("Uses threatening, abusive or insulting words or behaviour"),
  YOI_23_ADULT_20A("Uses threatening, abusive or insulting racist words or behaviour"),
  YOI_23_24_ADULT_20A_24("Uses threatening, abusive or insulting words or behaviour, which demonstrate, or are motivated (wholly or partly) by, hostility to persons based on them sharing a protected characteristic"),
  YOI_24_ADULT_21("Intentionally fails to work properly or, being required to work, refuses to do so"),
  YOI_25_ADULT_22("Disobeys any lawful order"),
  YOI_26_ADULT_23("Disobeys or fails to comply with any rule or regulation applying to ${PronounTypes.SUBJECT_PERSONAL.tag}"),
  YOI_26A_24_ADULT_23A_24("Fails to comply with any payback punishment"),
  DEFAULT(""),
  ;

  fun getParagraphDescription(gender: Gender): String {
    return formatGenderPronouns(
      description = this.description,
      gender = gender,
    )
  }

  companion object {

    private fun formatGenderPronouns(description: String, gender: Gender): String {
      var result: String = description
      gender.pronouns.forEach {
        result = result.replace(it.type.tag, it.value)
      }
      return result
    }
  }
}
