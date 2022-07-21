package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

class AdultOffenceCodes {
  companion object {
    // This information is taken from the spreadsheet of offences related to the ticket this work was done under.
    // Specifically these adult offences are in the "Dev Copy" worksheet
    fun buildLookup(): Map<Int, OffenceCodeDetails> {
      val paragraph1 = OffenceCodeParagraph("1", "Commits any assault")
      val paragraph2 = OffenceCodeParagraph("2", "Detains any person against their will")
      val paragraph3 = OffenceCodeParagraph(
        "3",
        "Denies access to any part of the prison to any officer or any person (other than a prisoner) who is at the prison for the purpose of working there"
      )
      val paragraph4 = OffenceCodeParagraph("4", "Fights with any person")
      val paragraph5 = OffenceCodeParagraph(
        "5",
        "Intentionally endangers the health or personal safety of others or, by their conduct, is reckless whether such health or personal safety is endangered"
      )
      val paragraph6 = OffenceCodeParagraph(
        "6",
        "Intentionally obstructs an officer in the execution of his duty, or any person (other than a prisoner) who is at the prison for the purpose of working there, in the performance of their work\n"
      )
      val paragraph7 = OffenceCodeParagraph("7", "Escapes or absconds from legal custody")
      val paragraph8 = OffenceCodeParagraph(
        "8",
        "Failing to comply with conditions of release under rule 9 which relates to temporary release"
      )
      val paragraph9 = OffenceCodeParagraph(
        "9",
        "Is found with any substance in his urine which demonstrates that a controlled drug (or specified drug) has, whether in prison or while on temporary release under rule 9 (or rule 9A), been administered to him by himself or by another person (but subject to rule 52)"
      )
      val paragraph10 =
        OffenceCodeParagraph("10", "Is intoxicated as a consequence of knowingly consuming any alcoholic beverage")
      val paragraph11 = OffenceCodeParagraph(
        "11",
        "Knowingly consumes any alcoholic beverage other than that provided to them pursuant to a written order under rule 25 (alcohol and tobacco) (1)"
      )
      val paragraph12 = OffenceCodeParagraph(
        "12",
        "Has in his possession either (a) any unauthorised article, or (b) a greater quantity of any article than he is authorised to have"
      )
      val paragraph13 = OffenceCodeParagraph("13", "Sells or delivers to any person any unauthorised article")
      val paragraph14 = OffenceCodeParagraph(
        "14",
        "Sells or, without permission, delivers to any person any article which they are allowed to have only for their own use"
      )
      val paragraph15 =
        OffenceCodeParagraph("15", "Takes improperly any article belonging to another person or to a prison")
      val paragraph16 = OffenceCodeParagraph(
        "16",
        "Intentionally or recklessly sets fire to any part of a prison or any other property, whether or not their own"
      )
      val paragraph17 = OffenceCodeParagraph(
        "17",
        "Destroys or damages any part of a prison or any other property, other than their own"
      )
      val paragraph18 = OffenceCodeParagraph(
        "18",
        "Absents himself from any place he is required to be or is present at any place where they are not authorised to be"
      )
      val paragraph19 = OffenceCodeParagraph(
        "19",
        "Is disrespectful to any officer, or any person (other than a prisoner) who is at the prison for the purpose of working there, or any person visiting a prison"
      )
      val paragraph20 = OffenceCodeParagraph("20", "Uses threatening, abusive or insulting words or behaviour")
      val paragraph21 =
        OffenceCodeParagraph("21", "Intentionally fails to work properly or, being required to work, refuses to do so")
      val paragraph22 = OffenceCodeParagraph("22", "Disobeys any lawful order")
      val paragraph23 =
        OffenceCodeParagraph("23", "Disobeys or fails to comply with any rule or regulation applying to them")
      val paragraph24 = OffenceCodeParagraph(
        "24",
        "Receives any controlled drug, or, without the consent of an officer, any other article, during the course of a visit (not being an interview such as is mentioned in rule 38, which relates to visits from legal advisors)"
      )
      val paragraph24a = OffenceCodeParagraph(
        "24(a)",
        "Displays, attaches or draws on any part of a prison, or on any other property, threatening, abusive or insulting racist words, drawings, symbols or other material",
        "24a"
      )

      return OffenceCodeLookupBuilder().nomisPrefix("51:")
        .code(1001).nomisCodes("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1002).nomisCodes("1B").notCommittedOnOwnNomisCodes("25D").paragraph(paragraph1)
        .code(1003).nomisCodes("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1004).nomisCodes("1J").notCommittedOnOwnNomisCodes("25F").paragraph(paragraph1)
        .code(1005).nomisCodes("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1006).nomisCodes("1N").notCommittedOnOwnNomisCodes("25G").paragraph(paragraph1)
        .code(1021).nomisCodes("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1022).nomisCodes("1B").notCommittedOnOwnNomisCodes("25D").paragraph(paragraph1)
        .code(1007).nomisCodes("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1008).nomisCodes("1F").notCommittedOnOwnNomisCodes("25E").paragraph(paragraph1)
        .code(4001).nomisCodes("4").notCommittedOnOwnNomisCodes("4").paragraph(paragraph4)
        .code(5001).nomisCodes("5").notCommittedOnOwnNomisCodes("25Q").paragraph(paragraph5)
        // Empty line in spreadsheet
        .code(7001).nomisCodes("7").notCommittedOnOwnNomisCodes("25O").paragraph(paragraph7)
        .code(7002).nomisCodes("7").notCommittedOnOwnNomisCodes("25O").paragraph(paragraph7)
        .code(8001).nomisCodes("8D").notCommittedOnOwnNomisCodes("25AI").paragraph(paragraph8)
        .code(8002).nomisCodes("8E").notCommittedOnOwnNomisCodes("25AJ").paragraph(paragraph8)
        // Empty line in spreadsheet
        .code(12001).nomisCodes("12").notCommittedOnOwnNomisCodes("25AM").paragraph(paragraph12)
        .code(12002).nomisCodes("12A").notCommittedOnOwnNomisCodes("25AH").paragraph(paragraph12)
        .code(14001).nomisCodes("14").notCommittedOnOwnNomisCodes("25J").paragraph(paragraph14)
        .code(13001).nomisCodes("13B").notCommittedOnOwnNomisCodes("25J").paragraph(paragraph13)
        .code(15001).nomisCodes("15").notCommittedOnOwnNomisCodes("25T").paragraph(paragraph15)
        // Empty line in spreadsheet
        .code(24001).nomisCodes("24").notCommittedOnOwnNomisCodes("25Y").paragraph(paragraph24)
        .code(24002).nomisCodes("24").notCommittedOnOwnNomisCodes("25Y").paragraph(paragraph24)
        .code(23001).nomisCodes("23AP").notCommittedOnOwnNomisCodes("25V").paragraph(paragraph23)
        .code(23002).nomisCodes("25Z").notCommittedOnOwnNomisCodes("25Z").paragraph(paragraph23)
        .code(9001).nomisCodes("9").notCommittedOnOwnNomisCodes("25U").paragraph(paragraph9)
        .code(9002).nomisCodes("9").notCommittedOnOwnNomisCodes("25U").paragraph(paragraph9)
        .code(12101).nomisCodes("12AQ").notCommittedOnOwnNomisCodes("25X").paragraph(paragraph12)
        .code(12102).nomisCodes("12AQ").notCommittedOnOwnNomisCodes("25X").paragraph(paragraph12)
        .code(10001).nomisCodes("10").notCommittedOnOwnNomisCodes("10").paragraph(paragraph10)
        .code(11001).nomisCodes("11").notCommittedOnOwnNomisCodes("11").paragraph(paragraph11)
        // Empty line in spreadsheet
        .code(16001).nomisCodes("16").notCommittedOnOwnNomisCodes("25C").paragraph(paragraph16)
        .code(24101).nomisCodes("24A").notCommittedOnOwnNomisCodes("25L").paragraph(paragraph24a)
        .code(17001).nomisCodes("17").notCommittedOnOwnNomisCodes("17").paragraph(paragraph17)
        .code(17002).nomisCodes("17").notCommittedOnOwnNomisCodes("17").paragraph(paragraph17)
        // Empty line in spreadsheet
        .code(19001).nomisCodes("19B").notCommittedOnOwnNomisCodes("25AE").paragraph(paragraph19)
        .code(19002).nomisCodes("19C").notCommittedOnOwnNomisCodes("25AF").paragraph(paragraph19)
        .code(19003).nomisCodes("19A").notCommittedOnOwnNomisCodes("25D").paragraph(paragraph19)
        .code(20001).nomisCodes("20A").notCommittedOnOwnNomisCodes("20A").paragraph(paragraph20)
        .code(20002).nomisCodes("20").notCommittedOnOwnNomisCodes("25AC").paragraph(paragraph20)
        // Empty line in spreadsheet
        .code(22001).nomisCodes("22").notCommittedOnOwnNomisCodes("25W").paragraph(paragraph22)
        .code(23101).nomisCodes("23").notCommittedOnOwnNomisCodes("25AN").paragraph(paragraph23)
        // Empty line in spreadsheet
        .code(2001).nomisCodes("2A").notCommittedOnOwnNomisCodes("25K").paragraph(paragraph2)
        .code(2002).nomisCodes("2C").notCommittedOnOwnNomisCodes("25M").paragraph(paragraph2)
        .code(2003).nomisCodes("2D").notCommittedOnOwnNomisCodes("25N").paragraph(paragraph2)
        .code(2021).nomisCodes("2A").notCommittedOnOwnNomisCodes("25K").paragraph(paragraph2)
        .code(2004).nomisCodes("2B").notCommittedOnOwnNomisCodes("25L").paragraph(paragraph2)
        // Empty line in spreadsheet
        .code(3001).nomisCodes("3").notCommittedOnOwnNomisCodes("25H").paragraph(paragraph3)
        .code(6001).nomisCodes("6").notCommittedOnOwnNomisCodes("25R").paragraph(paragraph6)
        .code(23201).nomisCodes("23AP").notCommittedOnOwnNomisCodes("25V").paragraph(paragraph23)
        .code(23202).nomisCodes("25Z").notCommittedOnOwnNomisCodes("25Z").paragraph(paragraph23)
        // Empty line in spreadsheet
        .code(18001).nomisCodes("18A").notCommittedOnOwnNomisCodes("25B").paragraph(paragraph18)
        .code(18002).nomisCodes("18B").notCommittedOnOwnNomisCodes("25AO").paragraph(paragraph18)
        .code(21001).nomisCodes("21").notCommittedOnOwnNomisCodes("25AL").paragraph(paragraph21)
        .build()
    }
  }
}
