package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.Optional

@Component
class UserDetails : AuthenticationFacade {
  override val currentUsername: String?
    get() = when (val userPrincipal: Any? = getUserPrincipal()) {
      is String -> userPrincipal.toString()
      is UserDetails -> userPrincipal.username
      is Map<*, *> -> userPrincipal.get("username").toString()
      else -> null
    }

  private fun getUserPrincipal(): Any? {
    val authentication = Optional.ofNullable(SecurityContextHolder.getContext().authentication)

    return authentication.map { it.principal }.orElse(null)
  }
}
