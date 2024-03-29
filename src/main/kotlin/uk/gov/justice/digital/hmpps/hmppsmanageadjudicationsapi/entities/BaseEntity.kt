package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.Hibernate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
) {

  @CreatedDate
  @Column(name = "CREATE_DATETIME", nullable = false)
  var createDateTime: LocalDateTime? = null

  @CreatedBy
  @Column(name = "CREATE_USER_ID", nullable = false)
  var createdByUserId: String? = null

  @LastModifiedDate
  @Column(name = "MODIFY_DATETIME", nullable = false)
  var modifiedDateTime: LocalDateTime? = null

  @LastModifiedBy
  @Column(name = "MODIFY_USER_ID", nullable = false)
  var modifiedByUserId: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as BaseEntity
    return id != null && id == other.id
  }

  override fun hashCode(): Int = 1004284837
}
