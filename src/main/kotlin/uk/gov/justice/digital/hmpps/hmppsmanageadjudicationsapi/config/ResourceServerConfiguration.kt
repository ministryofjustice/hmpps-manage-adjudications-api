package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration {

  private class ActiveCaseLoadFilter : Filter {
    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
      val httpRequest = request as? HttpServletRequest
      val activeCaseload = httpRequest?.getHeader(ACTIVE_CASELOAD)

      activeCaseload?.run {
        val sec = SecurityContextHolder.getContext().authentication as? AbstractAuthenticationToken
        sec?.details = mapOf(ACTIVE_CASELOAD to activeCaseload)
      }

      chain?.doFilter(request, response)
    }
  }

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http.headers().frameOptions().sameOrigin().and()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      // Can't have CSRF protection as requires session
      .and().csrf().disable()
      .authorizeHttpRequests { auth ->
        auth.requestMatchers(
          "/webjars/**", "/favicon.ico", "/csrf",
          "/health/**", "/info",
          "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
          "/swagger-resources", "/swagger-resources/configuration/ui", "/swagger-resources/configuration/security",
          "/draft-adjudications/orphaned",
        ).permitAll()
          .anyRequest()
          .authenticated()
      }.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
    return http.addFilterAfter(ActiveCaseLoadFilter(), BasicAuthenticationFilter::class.java).build()
  }

  companion object {
    const val ACTIVE_CASELOAD = "Active-Caseload"
  }
}
