package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security

import jakarta.validation.ValidationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ResourceServerConfiguration.Companion.ACTIVE_CASELOAD
import java.util.Optional

@Component
class UserDetails : AuthenticationFacade {
  override val currentUsername: String?
    get() = when (val userPrincipal: Any? = getUserPrincipal()) {
      is String -> userPrincipal.toString()
      is UserDetails -> userPrincipal.username
      is Map<*, *> -> userPrincipal["username"].toString()
      else -> null
    }
  override val activeCaseload: String
    get() = getActiveCaseloadFromSecurityContext() ?: throw ValidationException("no active caseload set")

  private fun getUserPrincipal(): Any? {
    val authentication = Optional.ofNullable(SecurityContextHolder.getContext().authentication)

    return authentication.map { it.principal }.orElse(null)
  }

  private fun getActiveCaseloadFromSecurityContext(): String? {
    val details = SecurityContextHolder.getContext().authentication?.details as? Map<*, *>

    return details?.get(ACTIVE_CASELOAD)?.toString()
  }
}
