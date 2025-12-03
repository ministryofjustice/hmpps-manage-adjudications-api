package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import com.fasterxml.jackson.databind.Module
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.JacksonModule
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature as Jackson2KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule as Jackson2KotlinModule

@Configuration
class JacksonConfig {

  // Jackson 3 Kotlin module for Spring Boot 4.0 Jackson 3 support
  @Bean
  fun kotlinModule(): JacksonModule = KotlinModule.Builder()
    .withReflectionCacheSize(512)
    .configure(KotlinFeature.NullToEmptyCollection, false)
    .configure(KotlinFeature.NullToEmptyMap, false)
    .configure(KotlinFeature.NullIsSameAsDefault, true)
    .configure(KotlinFeature.SingletonSupport, false)
    .configure(KotlinFeature.StrictNullChecks, false)
    .build()

  // Jackson 2 Kotlin module for backward compatibility with libraries still using Jackson 2
  @Bean
  fun jackson2KotlinModule(): Module = Jackson2KotlinModule.Builder()
    .withReflectionCacheSize(512)
    .configure(Jackson2KotlinFeature.NullToEmptyCollection, false)
    .configure(Jackson2KotlinFeature.NullToEmptyMap, false)
    .configure(Jackson2KotlinFeature.NullIsSameAsDefault, true)
    .configure(Jackson2KotlinFeature.SingletonSupport, false)
    .configure(Jackson2KotlinFeature.StrictNullChecks, false)
    .build()
}
