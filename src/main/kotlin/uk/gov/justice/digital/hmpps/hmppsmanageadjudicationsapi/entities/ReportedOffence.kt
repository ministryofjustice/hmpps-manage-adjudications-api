package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes

@Entity
@Table(name = "reported_offence")
data class ReportedOffence(
  override val id: Long? = null,
  var offenceCode: Int,
  @field:Length(max = 7)
  var victimPrisonersNumber: String? = null,
  @field:Length(max = 30)
  var victimStaffUsername: String? = null,
  @field:Length(max = 100)
  var victimOtherPersonsName: String? = null,
  var nomisOffenceCode: String? = null,
  @field:Length(max = 350)
  var nomisOffenceDescription: String? = null,
  var actualOffenceCode: Int? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_offence_fk_id")
  var protectedCharacteristics: MutableList<ProtectedCharacteristics> = mutableListOf(),

) : BaseEntity() {
  fun toDto(offenceCodeLookupService: OffenceCodeLookupService, isYouthOffender: Boolean, gender: Gender): OffenceDto {
    val offenceRuleDto = when (
      val offenceCode =
        offenceCodeLookupService.getOffenceCode(offenceCode = this.offenceCode, isYouthOffender = isYouthOffender)
    ) {
      OffenceCodes.MIGRATED_OFFENCE -> OffenceRuleDto(
        paragraphNumber = this.nomisOffenceCode!!,
        paragraphDescription = this.nomisOffenceDescription!!,
      )

      else -> OffenceRuleDto(
        paragraphNumber = offenceCode.paragraph,
        paragraphDescription = offenceCode.paragraphDescription.getParagraphDescription(gender),
        nomisCode = offenceCode.nomisCode,
        withOthersNomisCode = offenceCode.getNomisCodeWithOthers(),
      )
    }

    return OffenceDto(
      offenceCode = this.offenceCode,
      offenceRule = offenceRuleDto,
      victimPrisonersNumber = this.victimPrisonersNumber,
      victimStaffUsername = this.victimStaffUsername,
      victimOtherPersonsName = this.victimOtherPersonsName,
      protectedCharacteristics = this.protectedCharacteristics.map { it.characteristic },
    )
  }
}
