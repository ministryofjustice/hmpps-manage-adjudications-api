package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "draft_protected_characteristics")
data class DraftProtectedCharacteristics(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  var characteristic: Characteristic,
) : BaseEntity() {
  fun toProtectedCharacteristics(): ProtectedCharacteristics =
    ProtectedCharacteristics(characteristic = this.characteristic)
}

@Entity
@Table(name = "protected_characteristics")
data class ProtectedCharacteristics(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  var characteristic: Characteristic,
) : BaseEntity() {
  fun toDraftProtectedCharacteristics(): DraftProtectedCharacteristics =
    DraftProtectedCharacteristics(characteristic = this.characteristic)
}

enum class Characteristic {
  AGE,
  DISABILITY,
  GENDER_REASSIGN,
  MARRIAGE_AND_CP,
  PREGNANCY_AND_MAT,
  RACE,
  RELIGION,
  SEX,
  SEX_ORIENTATION,
}
