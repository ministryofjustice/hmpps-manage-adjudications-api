package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

class YouthOffenceCodes {
  companion object {
    // This information is taken from the spreadsheet of offences related to the ticket this work was done under.
    // Specifically these adult offences are in the "Dev Copy YO" worksheet
    fun buildLookup(): Map<Int, OffenceCodeDetails> {
      val paragraph1 = OffenceCodeParagraph("1", "Commits any assault")
      val paragraph3 = OffenceCodeParagraph("3", "Detains any person against their will")
      val paragraph4 = OffenceCodeParagraph(
        "4",
        "Denies access to any part of the young offender institution to any officer or any person (other than an inmate) who is at the young offender institution for the purpose of working there"
      )
      val paragraph5 = OffenceCodeParagraph("5", "Fights with any person")
      val paragraph6 = OffenceCodeParagraph(
        "6",
        "Intentionally endangers the health or personal safety of others or, by their conduct, is reckless whether such health or personal safety is endangered"
      )
      val paragraph7 = OffenceCodeParagraph(
        "7",
        "Intentionally obstructs an officer in the execution of his duty, or any person (other than an inmate) who is at the young offender institution for the purpose of working there, in the performance of his work"
      )
      val paragraph8 = OffenceCodeParagraph("8", "Escapes or absconds from a young offender institution or from legal custody")
      val paragraph9 = OffenceCodeParagraph(
        "9",
        "Failing to comply with conditions of release under rule 5 which relates to temporary release"
      )
      val paragraph10 = OffenceCodeParagraph(
        "10",
        "Administers a controlled drug to themself or fails to prevent the administration of a controlled drug to them by another person (but subject to rule 52 - defences to rule 51(9))"
      )
      val paragraph11 =
        OffenceCodeParagraph("11", "Is intoxicated as a consequence of knowingly consuming any alcoholic beverage")
      val paragraph12 = OffenceCodeParagraph(
        "12",
        "Knowingly consumes any alcoholic beverage other than that provided to them pursuant to a written order under rule 21 (alcohol and tobacco) (1)"
      )
      val paragraph13 = OffenceCodeParagraph(
        "13",
        "Has in his possession either (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have"
      )
      val paragraph14 = OffenceCodeParagraph(
        "14",
        "Sells or delivers to any person any unauthorised article"
      )
      val paragraph15 = OffenceCodeParagraph(
        "15",
        "Sells or, without permission, delivers to any person any article which they are allowed to have only for their own use"
      )
      val paragraph16 =
        OffenceCodeParagraph("16", "Takes improperly any article belonging to another person or to a young offender institution")
      val paragraph17 = OffenceCodeParagraph(
        "17",
        "Intentionally or recklessly sets fire to any part of a young offender institution or any other property, whether or not his own"
      )
      val paragraph19 = OffenceCodeParagraph(
        "19",
        "Displays, attaches or draws on any part of a prison, or on any other property, threatening, abusive or insulting racist words, drawings, symbols or other material",
      )
      val paragraph20 = OffenceCodeParagraph(
        "20",
        "Absents himself from any place he is required to be or is present at any place where they are not authorised to be"
      )
      val paragraph21 = OffenceCodeParagraph(
        "21",
        "Is disrespectful to any officer, or any person (other than a prisoner) who is at the prison for the purpose of working there, or any person visiting a prison"
      )
      val paragraph22 = OffenceCodeParagraph("22", "Uses threatening, abusive or insulting words or behaviour")
      val paragraph23 = OffenceCodeParagraph("23", "Uses threatening, abusive or insulting words or behaviour")
      val paragraph24 =
        OffenceCodeParagraph("24", "Intentionally fails to work properly or, being required to work, refuses to do so")
      val paragraph25 = OffenceCodeParagraph("25", "Disobeys any lawful order")
      val paragraph26 =
        OffenceCodeParagraph("26", "Disobeys or fails to comply with any rule or regulation applying to them")
      val paragraph27 = OffenceCodeParagraph(
        "27",
        "Receives any controlled drug, or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 16)"
      )

      return OffenceCodeLookupBuilder().nomisPrefix("55:")
        .code(1001).nomisCodes("2").notCommittedOnOwnNomisCodes("2").paragraph(paragraph1)
        .code(1002).nomisCodes("1A").notCommittedOnOwnNomisCodes("29D").paragraph(paragraph1)
        .code(1003).nomisCodes("2").notCommittedOnOwnNomisCodes("2").paragraph(paragraph1)
        .code(1004).nomisCodes("1L").notCommittedOnOwnNomisCodes("29F").paragraph(paragraph1)
        .code(1005).nomisCodes("2").notCommittedOnOwnNomisCodes("2").paragraph(paragraph1)
        .code(1006).nomisCodes("1M").notCommittedOnOwnNomisCodes("29G").paragraph(paragraph1)
        .code(1007).nomisCodes("2").notCommittedOnOwnNomisCodes("2").paragraph(paragraph1)
        .code(1008).nomisCodes("1E").notCommittedOnOwnNomisCodes("29E").paragraph(paragraph1)
        .code(4001).nomisCodes("5").notCommittedOnOwnNomisCodes("5").paragraph(paragraph5)
        .code(5001).nomisCodes("6").notCommittedOnOwnNomisCodes("29Q").paragraph(paragraph6)
        // Empty line in spreadsheet
        .code(7001).nomisCodes("8").notCommittedOnOwnNomisCodes("29O").paragraph(paragraph8)
        .code(7002).nomisCodes("8").notCommittedOnOwnNomisCodes("29O").paragraph(paragraph8)
        .code(8001).nomisCodes("9D").notCommittedOnOwnNomisCodes("29AI").paragraph(paragraph9)
        .code(8002).nomisCodes("9E").notCommittedOnOwnNomisCodes("29AJ").paragraph(paragraph9)
        // Empty line in spreadsheet
        .code(12001).nomisCodes("13").notCommittedOnOwnNomisCodes("29AM").paragraph(paragraph13)
        .code(12002).nomisCodes("13A").notCommittedOnOwnNomisCodes("29AH").paragraph(paragraph13)
        .code(14001).nomisCodes("15").notCommittedOnOwnNomisCodes("29J").paragraph(paragraph15)
        .code(13001).nomisCodes("14B").notCommittedOnOwnNomisCodes("29J").paragraph(paragraph14)
        .code(15001).nomisCodes("16").notCommittedOnOwnNomisCodes("29T").paragraph(paragraph16)
        // Empty line in spreadsheet
        .code(24001).nomisCodes("27").notCommittedOnOwnNomisCodes("29Y").paragraph(paragraph27)
        .code(24002).nomisCodes("27").notCommittedOnOwnNomisCodes("29Y").paragraph(paragraph27)
        .code(23001).nomisCodes("26B").notCommittedOnOwnNomisCodes("29V").paragraph(paragraph26)
        .code(23002).nomisCodes("26C").notCommittedOnOwnNomisCodes("29Z").paragraph(paragraph26)
        .code(9001).nomisCodes("10").notCommittedOnOwnNomisCodes("29U").paragraph(paragraph10)
        .code(9002).nomisCodes("10").notCommittedOnOwnNomisCodes("29U").paragraph(paragraph10)
        .code(12101).nomisCodes("13C").notCommittedOnOwnNomisCodes("29X").paragraph(paragraph13)
        .code(12102).nomisCodes("13B").notCommittedOnOwnNomisCodes("29X").paragraph(paragraph13)
        .code(10001).nomisCodes("11").notCommittedOnOwnNomisCodes("11").paragraph(paragraph11)
        .code(11001).nomisCodes("12").notCommittedOnOwnNomisCodes("12").paragraph(paragraph12)
        // Empty line in spreadsheet
        .code(16001).nomisCodes("17").notCommittedOnOwnNomisCodes("29C").paragraph(paragraph17)
        .code(24101).nomisCodes("19").notCommittedOnOwnNomisCodes("29I").paragraph(paragraph19)
        .code(17001).nomisCodes("17").notCommittedOnOwnNomisCodes("17").paragraph(paragraph17)
        .code(17002).nomisCodes("17").notCommittedOnOwnNomisCodes("17").paragraph(paragraph17)
        // Empty line in spreadsheet
        .code(19001).nomisCodes("21B").notCommittedOnOwnNomisCodes("29AE").paragraph(paragraph21)
        .code(19002).nomisCodes("21C").notCommittedOnOwnNomisCodes("29AF").paragraph(paragraph21)
        .code(19003).nomisCodes("21A").notCommittedOnOwnNomisCodes("29AD").paragraph(paragraph21)
        .code(20001).nomisCodes("23").notCommittedOnOwnNomisCodes("23").paragraph(paragraph23)
        .code(20002).nomisCodes("20").notCommittedOnOwnNomisCodes("29AC").paragraph(paragraph22)
        // Empty line in spreadsheet
        .code(22001).nomisCodes("25").notCommittedOnOwnNomisCodes("29W").paragraph(paragraph25)
        .code(23101).nomisCodes("26").notCommittedOnOwnNomisCodes("29AI").paragraph(paragraph26)
        // Empty line in spreadsheet
        .code(2001).nomisCodes("3A").notCommittedOnOwnNomisCodes("29K").paragraph(paragraph3)
        .code(2002).nomisCodes("3C").notCommittedOnOwnNomisCodes("29M").paragraph(paragraph3)
        .code(2003).nomisCodes("3D").notCommittedOnOwnNomisCodes("29N").paragraph(paragraph3)
        .code(2004).nomisCodes("3B").notCommittedOnOwnNomisCodes("29L").paragraph(paragraph3)
        // Empty line in spreadsheet
        .code(3001).nomisCodes("4").notCommittedOnOwnNomisCodes("29H").paragraph(paragraph4)
        .code(6001).nomisCodes("7").notCommittedOnOwnNomisCodes("29R").paragraph(paragraph7)
        .code(23201).nomisCodes("26B").notCommittedOnOwnNomisCodes("29V").paragraph(paragraph26)
        .code(23202).nomisCodes("26C").notCommittedOnOwnNomisCodes("29Z").paragraph(paragraph26)
        // Empty line in spreadsheet
        .code(18001).nomisCodes("20A").notCommittedOnOwnNomisCodes("29AB").paragraph(paragraph20)
        .code(18002).nomisCodes("20B").notCommittedOnOwnNomisCodes("29AO").paragraph(paragraph20)
        .code(21001).nomisCodes("24").notCommittedOnOwnNomisCodes("29AL").paragraph(paragraph24)
        .build()
    }
  }
}
